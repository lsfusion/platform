package platform.server.classes;

import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.ImValueMap;
import platform.server.caches.IdentityLazy;
import platform.server.caches.IdentityStrongLazy;
import platform.server.data.PropertyField;
import platform.server.data.SQLSession;
import platform.server.data.Table;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.SingleClassExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.query.Join;
import platform.server.data.query.QueryBuilder;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ObjectClassProperty;
import platform.server.logics.property.actions.ChangeClassValueActionProperty;
import platform.server.logics.table.ObjectTable;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class BaseClass extends AbstractCustomClass {

    protected final static Logger logger = Logger.getLogger(BaseClass.class);

    public ObjectTable table;

    public final UnknownClass unknown;
    public final AbstractCustomClass named;
    public final AbstractCustomClass sidClass;

    public StaticCustomClass objectClass;

    public BaseClass(String sID, String caption) {
        super(sID, caption);
        table = new ObjectTable(this);
        unknown = new UnknownClass(this);
        named = new AbstractCustomClass("named", ServerResourceBundle.getString("classes.named.object"), this);
        sidClass = new AbstractCustomClass("sidClass", ServerResourceBundle.getString("classes.static.object"), named);
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

        return findConcreteClassID((int)idClass);
    }

    public ConcreteCustomClass getConcrete() {
        MSet<ConcreteCustomClass> mConcrete = SetFact.mSet();
        fillNextConcreteChilds(mConcrete);
        return mConcrete.immutable().get(0);
    }

    public void initObjectClass() { // чтобы сохранить immutability классов
        MSet<CustomClass> mAllClasses = SetFact.mSet();
        fillChilds(mAllClasses); // именно так, а не getChilds, потому как добавление objectClass - тоже влияет на getChilds, и их нельзя кэшировать
        ImSet<CustomClass> allClasses = mAllClasses.immutable();

        // сначала обрабатываем baseClass.objectClass чтобы классы
        List<String> sidClasses = new ArrayList<String>();
        List<String> nameClasses = new ArrayList<String>();
        for(CustomClass customClass : allClasses)
            if(customClass instanceof ConcreteCustomClass) {
                sidClasses.add(customClass.getSID());
                nameClasses.add(customClass.caption);
            }
        objectClass = new StaticCustomClass("CustomObjectClass", ServerResourceBundle.getString("classes.object.class"), sidClass, sidClasses.toArray(new String[sidClasses.size()]), nameClasses.toArray(new String[nameClasses.size()]));
    }

    public void fillIDs(DataSession session, LCP name, LCP classSID, Map<String, String> sidChanges) throws SQLException {
        Set<CustomClass> allClasses = getChilds().toJavaSet();
        allClasses.remove(objectClass);

        Map<String, StaticCustomClass> usedSIds = new HashMap<String, StaticCustomClass>();
        Set<Integer> usedIds = new HashSet<Integer>();

        // baseClass'у и baseClass.objectClass'у нужны ID сразу потому как учавствуют в addObject
        ID = 0;
        named.ID = 1;
        sidClass.ID = 2;

        objectClass.ID = Integer.MAX_VALUE - 5; // в явную обрабатываем objectClass
        Integer classID = getClassID(objectClass.ID, session.sql);
        if(classID==null) {
            DataObject classObject = new DataObject(objectClass.ID, unknown);
            session.changeClass(classObject, objectClass);
            name.change(objectClass.caption, session, classObject);
            classSID.change(objectClass.sID, session, classObject);
        }
        usedSIds.put(objectClass.sID, objectClass);
        usedIds.add(objectClass.ID);

        Map<Object, String> modifiedSIDs = new HashMap<Object, String>();
        Map<Object, String> modifiedNames = objectClass.fillIDs(session, name, classSID, usedSIds, usedIds, sidChanges, modifiedSIDs);

        int free = 0;
        // пробежим по всем классам и заполним их ID
        for(CustomClass customClass : allClasses)
            if(customClass instanceof ConcreteCustomClass) {
                customClass.ID = objectClass.getID(customClass.getSID());
                free = Math.max(free, customClass.ID);
            }

        for (CustomClass customClass : allClasses) // заполним все остальные StaticClass
            if (customClass instanceof StaticCustomClass)
                modifiedNames.putAll(((StaticCustomClass) customClass).fillIDs(session, name, classSID, usedSIds, usedIds, sidChanges, modifiedSIDs));

        for (CustomClass customClass : allClasses)
            if (customClass instanceof AbstractCustomClass) {
                while (usedIds.contains(free))
                    free++;
                customClass.ID = free++;
            }

        for (Object object : modifiedSIDs.keySet()) {
            logger.info("changing sid of class with id " + object + " to " + modifiedSIDs.get(object));
            classSID.change(modifiedSIDs.get(object), session, session.getDataObject(object, ObjectType.instance));
        }

        // применение переименования классов вынесено сюда, поскольку objectClass.fillIDs() вызывается раньше проставления ID'шников - не срабатывает execute()
        for (Object object : modifiedNames.keySet()) {
            logger.info("renaming class with id " + object + " to " + modifiedNames.get(object));
            name.change(modifiedNames.get(object), session, session.getDataObject(object, ObjectType.instance));
        }
    }

    public void updateClassStat(SQLSession session) throws SQLException {
        QueryBuilder<Integer, Integer> classes = new QueryBuilder<Integer, Integer>(SetFact.singleton(0));

        KeyExpr countKeyExpr = new KeyExpr("count");
        Expr countExpr = GroupExpr.create(MapFact.singleton(0, countKeyExpr.classExpr(this)),
                new ValueExpr(1, IntegerClass.instance), countKeyExpr.isClass(this.getUpSet()), GroupType.SUM, classes.getMapExprs());

        classes.addProperty(0, countExpr);
        classes.and(classes.getMapExprs().get(0).isClass(objectClass));

        ImOrderMap<ImMap<Integer, Object>, ImMap<Integer, Object>> classStats = classes.execute(session);
        for(int i=0,size=classStats.size();i<size;i++) {
            CustomClass customClass = findClassID((int) (Integer) classStats.getKey(i).get(0));
            if(customClass instanceof CustomObjectClass) {
                Integer count = BaseUtils.nvl((Integer) classStats.getValue(i).get(0), 0);
                ((CustomObjectClass)customClass).stat = count==0?1:count;
            }
        }
    }

    public Integer getClassID(Integer value, SQLSession session) throws SQLException {
        QueryBuilder<Object,String> query = new QueryBuilder<Object,String>(MapFact.<Object, KeyExpr>EMPTYREV());
        Join<PropertyField> joinTable = table.joinAnd(MapFact.singleton(table.key, new ValueExpr(value, getConcrete())));
        query.and(joinTable.getWhere());
        query.addProperty("classid", joinTable.getExpr(table.objectClass));
        ImOrderMap<ImMap<Object, Object>, ImMap<String, Object>> result = query.execute(session);
        if(result.size()==0)
            return null;
        else {
            assert (result.size()==1);
            return (Integer) result.singleValue().get("classid");
        }
    }

    public Table.Join.Expr getJoinExpr(SingleClassExpr expr) {
        return (Table.Join.Expr) table.joinAnd(
                MapFact.singleton(table.key, expr)).getExpr(table.objectClass);
    }

    public int getCount() {
        return getUpSet().getCount();
    }

    @IdentityStrongLazy // equals'а нет
    public ObjectClassProperty getObjectClassProperty() {
        return new ObjectClassProperty("objectClass", this);
    }

    public DataObject getDataObject(SQLSession sql, Object value, Type type) throws SQLException {
        return new DataObject(value,type.getDataClass(value, sql, this));
    }

    public <K> ImMap<K, DataObject> getDataObjects(SQLSession sql, ImMap<K, Object> values, Type.Getter<K> typeGetter) throws SQLException {
        ImValueMap<K, DataObject> mvResult = values.mapItValues(); // exception кидается
        for(int i=0,size=values.size();i<size;i++)
            mvResult.mapValue(i, getDataObject(sql, values.getValue(i), typeGetter.getType(values.getKey(i))));
        return mvResult.immutableValue();
    }

    public ObjectValue getObjectValue(SQLSession sql, Object value, Type type) throws SQLException {
        if(value==null)
            return NullValue.instance;
        else
            return getDataObject(sql, value, type);
    }

    public <K> ImMap<K, ObjectValue> getObjectValues(SQLSession sql, ImMap<K, Object> values, Type.Getter<K> typeGetter) throws SQLException {
        ImValueMap<K, ObjectValue> mvResult = values.mapItValues(); // exception кидается
        for(int i=0,size=values.size();i<size;i++)
            mvResult.mapValue(i, getObjectValue(sql, values.getValue(i), typeGetter.getType(values.getKey(i))));
        return mvResult.immutableValue();
    }

    @IdentityStrongLazy // для ID
    public ChangeClassValueActionProperty getChangeClassValueAction() {
        return new ChangeClassValueActionProperty("CHANGE_CLASS_VALUE", ServerResourceBundle.getString("logics.property.actions.changeclass"), this);
    }
}
