package lsfusion.server.classes;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.ServerLoggers;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.OrObjectClassSet;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.IsClassExpr;
import lsfusion.server.data.expr.IsClassType;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.ObjectClassField;
import lsfusion.server.logics.property.ObjectClassProperty;
import lsfusion.server.physics.exec.table.FullTablesInterface;
import lsfusion.server.physics.exec.table.ImplementTable;
import lsfusion.server.session.DataSession;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.*;

public class BaseClass extends AbstractCustomClass {

    protected final static Logger logger = Logger.getLogger(BaseClass.class);
    private static final Logger startLogger = ServerLoggers.startLogger;

    public final UnknownClass unknown;

    public final AbstractCustomClass staticObjectClass;
    public ConcreteCustomClass objectClass;

    public FullTablesInterface fullTables;
    public void initFullTables(FullTablesInterface fullTables) {
        this.fullTables = fullTables;
    }

    public BaseClass(String canonicalName, LocalizedString caption, String staticCanonicalName, LocalizedString staticCanonicalCaption, Version version) {
        super(canonicalName, caption, version, ListFact.<CustomClass>EMPTY());
        unknown = new UnknownClass(this);
        staticObjectClass = new AbstractCustomClass(staticCanonicalName, staticCanonicalCaption, version, ListFact.singleton((CustomClass) this));
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
        objectClass = new ConcreteCustomClass(canonicalName, LocalizedString.create("{classes.object.class}"), version, ListFact.singleton((CustomClass) staticObjectClass));

        ImSet<CustomClass> allClasses = getAllClasses().remove(SetFact.singleton(objectClass));

        // сначала обрабатываем baseClass.objectClass чтобы классы
        List<String> sidClasses = new ArrayList<>();
        List<LocalizedString> nameClasses = new ArrayList<>();
        for(CustomClass customClass : allClasses)
            if(customClass instanceof ConcreteCustomClass) {
                sidClasses.add(customClass.getSID());
                nameClasses.add(customClass.caption);
            }
        ConcreteCustomClass.fillObjectClass(objectClass, sidClasses, nameClasses, version);
    }

    public void fillIDs(DataSession session, LCP staticCaption, LCP staticName, Map<String, String> sidChanges, Map<String, String> objectSIDChanges) throws SQLException, SQLHandledException {
        Map<String, ConcreteCustomClass> usedSIds = new HashMap<>();
        Set<Long> usedIds = new HashSet<>();

        // baseClass'у и baseClass.objectClass'у нужны ID сразу потому как учавствуют в addObject
        ID = 0L;

        objectClass.ID = Long.MAX_VALUE - 5; // в явную обрабатываем objectClass

        if(objectClass.readData(objectClass.ID, session.sql) == null) {
            DataObject classObject = new DataObject(objectClass.ID, unknown);
            session.changeClass(classObject, objectClass);
            staticCaption.change(ThreadLocalContext.localize(objectClass.caption), session, classObject);
            staticName.change(objectClass.getSID(), session, classObject);
        }
        usedSIds.put(objectClass.getSID(), objectClass);
        usedIds.add(objectClass.ID);

        Map<DataObject, String> modifiedSIDs = new HashMap<>();
        Map<DataObject, String> modifiedCaptions = objectClass.fillIDs(session, staticCaption, staticName, usedSIds, usedIds, sidChanges, modifiedSIDs);

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
                modifiedCaptions.putAll(((ConcreteCustomClass) customClass).fillIDs(session, staticCaption, staticName, usedSIds, usedIds, objectSIDChanges, modifiedSIDs));

        for (CustomClass customClass : allClasses)
            if (customClass instanceof AbstractCustomClass) {
                while (usedIds.contains(free))
                    free++;
                customClass.ID = free++;
            }

        for (Map.Entry<DataObject, String> modifiedSID : modifiedSIDs.entrySet()) {
            startLogger.info("changing sid of class with id " + modifiedSID.getKey() + " to " + modifiedSID.getValue());
            staticName.change(modifiedSID.getValue(), session, modifiedSID.getKey());
        }

        // применение переименования классов вынесено сюда, поскольку objectClass.fillIDs() вызывается раньше проставления ID'шников - не срабатывает execute()
        for (Map.Entry<DataObject, String> modifiedCaption : modifiedCaptions.entrySet()) {
            startLogger.info("renaming class with id " + modifiedCaption.getKey() + " to " + modifiedCaption.getValue());
            staticCaption.change(modifiedCaption.getValue(), session, modifiedCaption.getKey());
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
    @IdentityLazy
    public Pair<KeyExpr, Expr> getSubQuery(ImSet<ObjectClassField> classTables, IsClassType type) {
        KeyExpr keyExpr = new KeyExpr("isSetClass");
        return new Pair<>(keyExpr, IsClassExpr.getTableExpr(keyExpr, classTables, IsClassExpr.subqueryThreshold, type));
    }
    @IdentityStrongLazy
    public NamedTable getInconsistentTable(ImplementTable table) {
        return table.getInconsistent(this);
    }

    @Override
    public boolean hasComplex() {
        return false;
    }
}
