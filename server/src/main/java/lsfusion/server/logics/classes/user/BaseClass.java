package lsfusion.server.logics.classes.user;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.IsClassExpr;
import lsfusion.server.data.expr.classes.IsClassType;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.OrObjectClassSet;
import lsfusion.server.logics.property.classes.user.ObjectClassProperty;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.DBTable;
import lsfusion.server.physics.exec.db.table.FullTablesInterface;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.*;

public class BaseClass extends AbstractCustomClass {

    protected final static Logger logger = Logger.getLogger(BaseClass.class);

    public final UnknownClass unknown;

    public final AbstractCustomClass staticObjectClass;
    public ConcreteCustomClass objectClass;

    public FullTablesInterface fullTables;
    public void initFullTables(FullTablesInterface fullTables) {
        this.fullTables = fullTables;
    }

    public BaseClass(String canonicalName, LocalizedString caption, String staticCanonicalName, LocalizedString staticCanonicalCaption, Version version) {
        super(canonicalName, caption, null, version, ListFact.EMPTY());
        unknown = new UnknownClass(this);
        staticObjectClass = new AbstractCustomClass(staticCanonicalName, staticCanonicalCaption, null, version, ListFact.singleton(this));
    }

    @Override
    public BaseClass getBaseClass() {
        return this;
    }

    public ObjectClass findClassID(Long idClass) {
        if(idClass==null) return unknown;

        return findClassID((long)idClass);
    }

    // для того чтобы группировать по классам в некоторых местах делается nvl(-1), тем самым assert'ся что результат не null
    // но если "плывут" классы, или просто объект параллельно удалили, может нарушаться поэтому пока вставим assertion 
    public ConcreteObjectClass findConcreteClassID(Long id, long nullValue) {
        if(id == null) {
            id = nullValue;
            ServerLoggers.assertLog(false, "CLASS RESULT SHOULD NOT BE NULL");
        }
        return findConcreteClassID(id != nullValue ? id : null);        
    }

    @IdentityLazy
    public ConcreteObjectClass findConcreteClassID(Long idClass) {
        if(idClass==null) return unknown;

        return findConcreteClassID((long) idClass);
    }

    public ImSet<CustomClass> getAllClasses() {
        MSet<CustomClass> mAllClasses = SetFact.mSet();
        fillChilds(mAllClasses); // именно так, а не getChilds, потому как добавление objectClass - тоже влияет на getChilds, и их нельзя кэшировать
        return mAllClasses.immutable();
    }

    public void initObjectClass(Version version, String canonicalName) { // чтобы сохранить immutability классов
        objectClass = new ConcreteCustomClass(canonicalName, LocalizedString.create("{classes.object.class}"), null, version, ListFact.singleton(staticObjectClass));

        ImSet<CustomClass> allClasses = getAllClasses().remove(SetFact.singleton(objectClass));

        // сначала обрабатываем baseClass.objectClass чтобы классы
        List<String> sidClasses = new ArrayList<>();
        List<LocalizedString> nameClasses = new ArrayList<>();
        List<String> images = new ArrayList<>();
        for(CustomClass customClass : allClasses)
            if(customClass instanceof ConcreteCustomClass) {
                sidClasses.add(customClass.getSID());
                nameClasses.add(customClass.caption);
                images.add(customClass.image);
            }
        ConcreteCustomClass.fillObjectClass(objectClass, sidClasses, nameClasses, images, version);
    }

    public void fillIDs(SQLSession sql, QueryEnvironment env, SQLCallable<Long> idGen, LP staticCaption, LP staticImage, LP<?> staticName, Map<String, String> sidChanges, Map<String, String> objectSIDChanges, DBManager.IDChanges dbChanges) throws SQLException, SQLHandledException {
        Map<String, ConcreteCustomClass> usedSIds = new HashMap<>();
        Set<Long> usedIds = new HashSet<>();

        // baseClass'у и baseClass.objectClass'у нужны ID сразу потому как учавствуют в addObject
        ID = 0L;

        objectClass.ID = Long.MAX_VALUE - 5; // в явную обрабатываем objectClass

        if(objectClass.readData(objectClass.ID, sql, env) == null)
            dbChanges.added.add(new DBManager.IDAdd(objectClass.ID, objectClass, objectClass.getSID(), ThreadLocalContext.localize(objectClass.caption), "object"));

        usedSIds.put(objectClass.getSID(), objectClass);
        usedIds.add(objectClass.ID);

        objectClass.fillIDs(sql, env, idGen, staticCaption, staticImage, staticName, usedSIds, usedIds, sidChanges, dbChanges);

        Set<CustomClass> allClasses = getAllChildren().toJavaSet();
        allClasses.remove(objectClass);

        // пробежим по всем классам и заполним их ID
        for(CustomClass customClass : allClasses)
            if(customClass instanceof ConcreteCustomClass)
                customClass.ID = objectClass.getObjectID(customClass.getSID());

        long free = 0;
        for(CustomClass customClass : allClasses)
            if(customClass instanceof ConcreteCustomClass)
                free = Math.max(free, customClass.ID);

        for (CustomClass customClass : allClasses) // заполним все остальные StaticClass
            if (customClass instanceof ConcreteCustomClass)
                ((ConcreteCustomClass) customClass).fillIDs(sql, env, idGen, staticCaption, staticImage, staticName, usedSIds, usedIds, objectSIDChanges, dbChanges);

        for (CustomClass customClass : allClasses)
            if (customClass instanceof AbstractCustomClass) {
                while (usedIds.contains(free))
                    free++;
                customClass.ID = free++;
            }
    }

    public int getCount() {
        return getUpSet().getCount();
    }

    @IdentityStrongLazy // equals'а нет
    public ObjectClassProperty getObjectClassProperty() {
        return new ObjectClassProperty(this);
    }

    public DataObject getDataObject(SQLSession sql, Object value, AndClassSet classSet, OperationOwner owner) throws SQLException, SQLHandledException {
        return new DataObject(value, classSet.getType().getDataClass(value, sql, classSet, this, owner));
    }

    public <K> ImMap<K, DataObject> getDataObjects(SQLSession sql, ImMap<K, Object> values, ImMap<K, AndClassSet> classes, OperationOwner owner) throws SQLException, SQLHandledException {
        ImValueMap<K, DataObject> mvResult = values.mapItValues(); // exception кидается
        for(int i=0,size=values.size();i<size;i++)
            mvResult.mapValue(i, getDataObject(sql, values.getValue(i), classes.get(values.getKey(i)), owner));
        return mvResult.immutableValue();
    }

    public ObjectValue getObjectValue(SQLSession sql, Object value, AndClassSet type, OperationOwner owner) throws SQLException, SQLHandledException {
        if(value==null)
            return NullValue.instance;
        else
            return getDataObject(sql, value, type, owner);
    }

    public <K> ImMap<K, ObjectValue> getObjectValues(SQLSession sql, ImMap<K, Object> values, ImMap<K, AndClassSet> classes, OperationOwner owner) throws SQLException, SQLHandledException {
        ImValueMap<K, ObjectValue> mvResult = values.mapItValues(); // exception кидается
        for(int i=0,size=values.size();i<size;i++)
            mvResult.mapValue(i, getObjectValue(sql, values.getValue(i), classes.get(values.getKey(i)), owner));
        return mvResult.immutableValue();
    }

    @IdentityLazy
    public ObjectValueClassSet getSet(ImSet<ObjectClassField> classTables) {
        ObjectValueClassSet set = OrObjectClassSet.FALSE;
        for(ObjectClassField classTable : classTables)
            set = (ObjectValueClassSet) set.or(classTable.getObjectSet());
        return set;
    }
    @IdentityInstanceLazy
    public Pair<KeyExpr, Expr> getSubQuery(ImSet<ObjectClassField> classTables, IsClassType type) {
        KeyExpr keyExpr = new KeyExpr("isSetClass");
        return new Pair<>(keyExpr, IsClassExpr.getTableExpr(keyExpr, classTables, type));
    }
    @IdentityStrongLazy
    public DBTable getInconsistentTable(ImplementTable table) {
        return table.getInconsistent(this);
    }

    @Override
    public boolean hasComplex() {
        return false;
    }
}
