package lsfusion.server.classes;

import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.Table;
import lsfusion.server.data.expr.*;
import lsfusion.server.logics.table.ImplementTable;
import org.apache.log4j.Logger;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.ServerLoggers;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.OrObjectClassSet;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassField;
import lsfusion.server.logics.property.ObjectClassProperty;
import lsfusion.server.logics.property.actions.ChangeClassValueActionProperty;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class BaseClass extends AbstractCustomClass {

    protected final static Logger logger = Logger.getLogger(BaseClass.class);
    private static final Logger systemLogger = ServerLoggers.systemLogger;

    public final UnknownClass unknown;

    public ConcreteCustomClass objectClass;

    public BaseClass(String sID, String caption) {
        super(sID, caption);
        unknown = new UnknownClass(this);
    }

    @Override
    public BaseClass getBaseClass() {
        return this;
    }

    public ObjectClass findClassID(Integer idClass) {
        if(idClass==null) return unknown;

        return findClassID((int)idClass);
    }

    @IdentityLazy
    public ConcreteObjectClass findConcreteClassID(Integer idClass) {
        if(idClass==null) return unknown;

        return findConcreteClassID((int) idClass);
    }

    public ImSet<CustomClass> getAllClasses() {
        MSet<CustomClass> mAllClasses = SetFact.mSet();
        fillChilds(mAllClasses); // именно так, а не getChilds, потому как добавление objectClass - тоже влияет на getChilds, и их нельзя кэшировать
        return mAllClasses.immutable();
    }

    public void initObjectClass() { // чтобы сохранить immutability классов
        ImSet<CustomClass> allClasses = getAllClasses();

        // сначала обрабатываем baseClass.objectClass чтобы классы
        List<String> sidClasses = new ArrayList<String>();
        List<String> nameClasses = new ArrayList<String>();
        for(CustomClass customClass : allClasses)
            if(customClass instanceof ConcreteCustomClass) {
                sidClasses.add(customClass.getSID());
                nameClasses.add(customClass.caption);
            }
        objectClass = ConcreteCustomClass.createObjectClass("CustomObjectClass", ServerResourceBundle.getString("classes.object.class"), sidClasses, nameClasses, this);
    }

    public void fillIDs(DataSession session, LCP staticCaption, LCP staticName, Map<String, String> sidChanges, Map<String, String> objectSIDChanges) throws SQLException, SQLHandledException {
        Map<String, ConcreteCustomClass> usedSIds = new HashMap<String, ConcreteCustomClass>();
        Set<Integer> usedIds = new HashSet<Integer>();

        // baseClass'у и baseClass.objectClass'у нужны ID сразу потому как учавствуют в addObject
        ID = 0;

        objectClass.ID = Integer.MAX_VALUE - 5; // в явную обрабатываем objectClass
        if(objectClass.readData(objectClass.ID, session.sql) == null) {
            DataObject classObject = new DataObject(objectClass.ID, unknown);
            session.changeClass(classObject, objectClass);
            staticCaption.change(objectClass.caption, session, classObject);
            staticName.change(objectClass.sID, session, classObject);
        }
        usedSIds.put(objectClass.sID, objectClass);
        usedIds.add(objectClass.ID);

        Map<DataObject, String> modifiedSIDs = new HashMap<DataObject, String>();
        Map<DataObject, String> modifiedNames = objectClass.fillIDs(session, staticCaption, staticName, usedSIds, usedIds, sidChanges, modifiedSIDs);

        Set<CustomClass> allClasses = getAllChildren().toJavaSet();
        allClasses.remove(objectClass);

        // пробежим по всем классам и заполним их ID
        for(CustomClass customClass : allClasses)
            if(customClass instanceof ConcreteCustomClass)
                customClass.ID = objectClass.getObjectID(customClass.getSID());

        int free = 0;
        for(CustomClass customClass : allClasses)
            if(customClass instanceof ConcreteCustomClass)
                free = Math.max(free, customClass.ID);

        for (CustomClass customClass : allClasses) // заполним все остальные StaticClass
            if (customClass instanceof ConcreteCustomClass)
                modifiedNames.putAll(((ConcreteCustomClass) customClass).fillIDs(session, staticCaption, staticName, usedSIds, usedIds, objectSIDChanges, modifiedSIDs));

        for (CustomClass customClass : allClasses)
            if (customClass instanceof AbstractCustomClass) {
                while (usedIds.contains(free))
                    free++;
                customClass.ID = free++;
            }

        for (Map.Entry<DataObject, String> modifiedSID : modifiedSIDs.entrySet()) {
            systemLogger.info("changing sid of class with id " + modifiedSID.getKey() + " to " + modifiedSID.getValue());
            staticName.change(modifiedSID.getValue(), session, modifiedSID.getKey());
        }

        // применение переименования классов вынесено сюда, поскольку objectClass.fillIDs() вызывается раньше проставления ID'шников - не срабатывает execute()
        for (Map.Entry<DataObject, String> modifiedName : modifiedNames.entrySet()) {
            systemLogger.info("renaming class with id " + modifiedName.getKey() + " to " + modifiedName.getValue());
            staticCaption.change(modifiedName.getValue(), session, modifiedName.getKey());
        }
    }

    public void updateClassStat(SQLSession session) throws SQLException, SQLHandledException {
        for(ObjectValueClassSet tableClasses : getUpTables().valueIt()) {
            QueryBuilder<Integer, Integer> classes = new QueryBuilder<Integer, Integer>(SetFact.singleton(0));

            KeyExpr countKeyExpr = new KeyExpr("count");
            Expr countExpr = GroupExpr.create(MapFact.singleton(0, countKeyExpr.classExpr(this)),
                    new ValueExpr(1, IntegerClass.instance), countKeyExpr.isClass(tableClasses), GroupType.SUM, classes.getMapExprs());

            classes.addProperty(0, countExpr);
            classes.and(countExpr.getWhere());//classes.getMapExprs().get(0).isClass(objectClass));

            ImOrderMap<ImMap<Integer, Object>, ImMap<Integer, Object>> classStats = classes.execute(session, OperationOwner.unknown);
            ImSet<ConcreteCustomClass> concreteChilds = tableClasses.getSetConcreteChildren();
            for(int i=0,size=concreteChilds.size();i<size;i++) {
                ConcreteCustomClass customClass = concreteChilds.get(i);
                ImMap<Integer, Object> classStat = classStats.get(MapFact.singleton(0, (Object) customClass.ID));
                customClass.stat = classStat==null ? 1 : (Integer)classStat.singleValue();
            }
        }
    }

    public int getCount() {
        return getUpSet().getCount();
    }

    @IdentityStrongLazy // equals'а нет
    public ObjectClassProperty getObjectClassProperty() {
        return new ObjectClassProperty("objectClass", this);
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

    @IdentityStrongLazy // для ID
    public ChangeClassValueActionProperty getChangeClassValueAction() {
        return new ChangeClassValueActionProperty("CHANGE_CLASS_VALUE", ServerResourceBundle.getString("logics.property.actions.changeclass"), this);
    }

    @IdentityLazy
    public ObjectValueClassSet getSet(ImSet<ClassField> classTables) {
        ObjectValueClassSet set = OrObjectClassSet.FALSE;
        for(ClassField classTable : classTables)
            set = (ObjectValueClassSet) set.or(classTable.getObjectSet());
        return set;
    }
    @IdentityLazy
    public Pair<KeyExpr, Expr> getSubQuery(ImSet<ClassField> classTables, IsClassType type) {
        KeyExpr keyExpr = new KeyExpr("isSetClass");
        return new Pair<KeyExpr, Expr>(keyExpr, IsClassExpr.getTableExpr(keyExpr, classTables, IsClassExpr.subqueryThreshold, type));
    }
    @IdentityStrongLazy
    public Table getInconsistentTable(ImplementTable table) {
        return table.getInconsistent(this);
    }
}
