package platform.server.classes;

import platform.server.session.DataSession;
import platform.server.logics.linear.LP;
import platform.server.logics.DataObject;
import platform.server.data.query.Query;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.SystemValueExpr;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;

import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.sql.SQLException;

public class StaticCustomClass extends ConcreteCustomClass implements StaticClass {

    private String[] sids;
    public String[] names;
    public Integer[] ids;

    public StaticCustomClass(String sID, String caption, CustomClass sidClass, String[] sids, String[] names) {
        super(sID, caption, sidClass);

        this.sids = sids;
        this.names = names;
        this.ids = new Integer[sids.length];
    }

    public int getID(String sID) {
        for(int i=0;i<sids.length;i++)
            if(sids[i].equals(sID))
                return ids[i];
        throw new RuntimeException("sid not found");
    }

    public String getSID(int id) {
        for(int i=0;i<ids.length;i++)
            if(ids[i] == id)
                return sids[i];
        throw new RuntimeException("id not found");
    }

    public void fillIDs(DataSession session, LP name, LP classSID, Map<String, StaticCustomClass> usedSIds, Set<Integer> usedIds) throws SQLException {
        StaticCustomClass usedClass;
        for(int i = 0;i<sids.length;i++) {
            String sidObject = sids[i];
            if ((usedClass = usedSIds.put(sidObject, this)) != null)
                throw new RuntimeException("Одинаковый идентификатор " + sidObject + " у объектов классов " + caption + " и " + usedClass.caption);

            // ищем класс с таким sID, если не находим создаем
            Query<String, Object> findClass = new Query<String, Object>(Collections.singleton("key"));
            findClass.and(classSID.getExpr(session.modifier, BaseUtils.singleValue(findClass.mapKeys)).compare(new ValueExpr(sidObject, StringClass.get(sidObject.length())), Compare.EQUALS));
            OrderedMap<Map<String, Object>, Map<Object, Object>> result = findClass.execute(session.sql, session.env);
            if (result.size() == 0) { // не найдено добавляем новый объект и заменяем ему classID и title
                DataObject classObject = session.addObject(this, session.modifier);
                name.execute(names[i], session, session.modifier, classObject);
                classSID.execute(sidObject, session, session.modifier, classObject);
                ids[i] = (Integer) classObject.object;
            } else // assert'ся что класс 1
                ids[i] = (Integer) BaseUtils.singleKey(result).get("key");

            usedIds.add(ids[i]);
        }
    }

    public Expr getStaticExpr(Object value) {
        return new SystemValueExpr(getID((String)value), this);
    }
}
