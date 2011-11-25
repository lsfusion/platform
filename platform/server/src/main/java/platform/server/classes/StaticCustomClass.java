package platform.server.classes;

import platform.base.ArrayInstancer;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.data.expr.Expr;
import platform.server.data.expr.StaticValueExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.Query;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LP;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class StaticCustomClass extends ConcreteCustomClass implements StaticClass {

    private String[] sids;
    public String[] names;
    public Integer[] ids;

    public StaticCustomClass(String sID, String caption, CustomClass sidClass, String[] sids, String[] names, CustomClass... parents) {
        super(sID, caption, BaseUtils.addElement(parents, sidClass, new ArrayInstancer<CustomClass>() {
            @Override
            public CustomClass[] newArray(int size) {
                return new CustomClass[size];
            }
        }));

        this.sids = sids;
        this.names = names;
    }

    public boolean hasSID(String sID) {
        for (String classSID : sids) {
            if (classSID.equals(sID)) {
                return true;
            }
        }
        return false;
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
        ids = new Integer[sids.length];
        for(int i = 0;i<sids.length;i++) {
            String sidObject = sids[i];
            if ((usedClass = usedSIds.put(sidObject, this)) != null)
                throw new RuntimeException(ServerResourceBundle.getString("classes.objects.have.the.same.id", sidObject, caption, usedClass.caption));

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
        return new StaticValueExpr(value, this, true);
    }

    public DataObject getDataObject(String sID) {
        return new DataObject(getID(sID), this);
    }
    public String getString(Object value, SQLSyntax syntax) {
        return getDataObject((String)value).getString(syntax);
    }

    public int getCount() {
        return sids.length;
    }
}
