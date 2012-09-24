package platform.server.classes;

import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.caches.IdentityLazy;
import platform.server.classes.sets.ConcreteCustomClassSet;
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
import platform.server.data.query.Query;
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
        ConcreteCustomClassSet concrete = new ConcreteCustomClassSet();
        fillNextConcreteChilds(concrete);
        return concrete.get(0);
    }

    public void initObjectClass() { // чтобы сохранить immutability классов
        Set<CustomClass> allClasses = new HashSet<CustomClass>();
        fillChilds(allClasses);

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

    public void fillIDs(DataSession session, LCP name, LCP classSID) throws SQLException {
        Set<CustomClass> allClasses = new HashSet<CustomClass>();
        fillChilds(allClasses);
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

        Map<Object, String> modifiedNames = objectClass.fillIDs(session, name, classSID, usedSIds, usedIds);

        // пробежим по всем классам и заполним их ID
        for(CustomClass customClass : allClasses)
            if(customClass instanceof ConcreteCustomClass)
                customClass.ID = objectClass.getID(customClass.getSID());

        for (CustomClass customClass : allClasses) // заполним все остальные StaticClass
            if (customClass instanceof StaticCustomClass)
                modifiedNames.putAll(((StaticCustomClass) customClass).fillIDs(session, name, classSID, usedSIds, usedIds));

        int free = 0;
        for (CustomClass customClass : allClasses)
            if (customClass instanceof AbstractCustomClass) {
                while (usedIds.contains(free))
                    free++;
                customClass.ID = free++;
            }

        // применение переименования классов вынесено сюда, поскольку objectClass.fillIDs() вызывается раньше проставления ID'шников - не срабатывает execute()
        for (Object object : modifiedNames.keySet()) {
            logger.info("renaming class with id " + object + " to " + modifiedNames.get(object));
            name.change(modifiedNames.get(object), session, session.getDataObject(object, ObjectType.instance));
        }
    }

    public void updateClassStat(SQLSession session) throws SQLException {
        Query<Integer, Integer> classes = new Query<Integer, Integer>(Collections.singleton(0));

        KeyExpr countKeyExpr = new KeyExpr("count");
        Expr countExpr = GroupExpr.create(Collections.singletonMap(0, countKeyExpr.classExpr(this)),
                new ValueExpr(1, IntegerClass.instance), countKeyExpr.isClass(this.getUpSet()), GroupType.SUM, classes.mapKeys);

        classes.properties.put(0, countExpr);
        classes.and(classes.mapKeys.get(0).isClass(objectClass));

        OrderedMap<Map<Integer, Object>, Map<Integer, Object>> classStats = classes.execute(session);
        for(Map.Entry<Map<Integer, Object>, Map<Integer, Object>> classStat : classStats.entrySet()) {
            CustomClass customClass = findClassID((int) (Integer) classStat.getKey().get(0));
            if(customClass instanceof CustomObjectClass) {
                Integer count = BaseUtils.nvl((Integer) classStat.getValue().get(0), 0);
                ((CustomObjectClass)customClass).stat = count==0?1:count;
            }
        }
    }

    public Integer getClassID(Integer value, SQLSession session) throws SQLException {
        Query<Object,String> query = new Query<Object,String>(new HashMap<Object, KeyExpr>());
        Join<PropertyField> joinTable = table.joinAnd(Collections.singletonMap(table.key,new ValueExpr(value,getConcrete())));
        query.and(joinTable.getWhere());
        query.properties.put("classid", joinTable.getExpr(table.objectClass));
        OrderedMap<Map<Object, Object>, Map<String, Object>> result = query.execute(session);
        if(result.size()==0)
            return null;
        else {
            assert (result.size()==1);
            return (Integer) result.singleValue().get("classid");
        }
    }

    public Table.Join.Expr getJoinExpr(SingleClassExpr expr) {
        return (Table.Join.Expr) table.joinAnd(
                Collections.singletonMap(table.key, expr)).getExpr(table.objectClass);
    }

    public int getCount() {
        return getUpSet().getCount();
    }

    @IdentityLazy
    public ObjectClassProperty getObjectClassProperty() {
        return new ObjectClassProperty("objectClass", this);
    }

    public DataObject getDataObject(SQLSession sql, Object value, Type type) throws SQLException {
        return new DataObject(value,type.getDataClass(value, sql, this));
    }

    public <K> Map<K, DataObject> getDataObjects(SQLSession sql, Map<K, Object> values, Type.Getter<K> typeGetter) throws SQLException {
        Map<K, DataObject> result = new HashMap<K, DataObject>();
        for(Map.Entry<K, Object> value : values.entrySet())
            result.put(value.getKey(), getDataObject(sql, value.getValue(), typeGetter.getType(value.getKey())));
        return result;
    }

    public ObjectValue getObjectValue(SQLSession sql, Object value, Type type) throws SQLException {
        if(value==null)
            return NullValue.instance;
        else
            return getDataObject(sql, value, type);
    }

    public <K> Map<K, ObjectValue> getObjectValues(SQLSession sql, Map<K, Object> values, Type.Getter<K> typeGetter) throws SQLException {
        Map<K, ObjectValue> result = new HashMap<K, ObjectValue>();
        for(Map.Entry<K, Object> value : values.entrySet())
            result.put(value.getKey(), getObjectValue(sql, value.getValue(), typeGetter.getType(value.getKey())));
        return result;
    }

    @IdentityLazy
    public ChangeClassValueActionProperty getChangeClassValueAction() {
        return new ChangeClassValueActionProperty("CHANGE_CLASS_VALUE", ServerResourceBundle.getString("logics.property.actions.changeclass"), this);
    }
}
