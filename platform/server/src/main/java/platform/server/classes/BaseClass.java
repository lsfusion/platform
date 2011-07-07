package platform.server.classes;

import platform.server.caches.IdentityLazy;
import platform.server.classes.sets.ConcreteCustomClassSet;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.table.ObjectTable;
import platform.server.logics.linear.LP;
import platform.server.logics.DataObject;
import platform.server.session.DataSession;
import platform.server.data.SQLSession;
import platform.server.data.PropertyField;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.Query;
import platform.server.data.query.Join;
import platform.base.OrderedMap;

import java.util.*;
import java.sql.SQLException;

public class BaseClass extends AbstractCustomClass {

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

    public void fillIDs(DataSession session, LP name, LP classSID) throws SQLException {
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
            name.execute(objectClass.caption, session, session.modifier, classObject);
            classSID.execute(objectClass.sID, session, session.modifier, classObject);
        }
        usedSIds.put(objectClass.sID, objectClass);
        usedIds.add(objectClass.ID);

        objectClass.fillIDs(session, name, classSID, usedSIds, usedIds);

        // пробежим по всем классам и заполним их ID
        for(CustomClass customClass : allClasses)
            if(customClass instanceof ConcreteCustomClass)
                customClass.ID = objectClass.getID(customClass.getSID());

        for (CustomClass customClass : allClasses) // заполним все остальные StaticClass
            if (customClass instanceof StaticCustomClass)
                ((StaticCustomClass) customClass).fillIDs(session, name, classSID, usedSIds, usedIds);

        int free = 0;
        for (CustomClass customClass : allClasses)
            if (customClass instanceof AbstractCustomClass) {
                while (usedIds.contains(free))
                    free++;
                customClass.ID = free++;
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
}
