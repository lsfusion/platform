package platform.server.classes;

import platform.base.ArrayInstancer;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Pair;
import platform.server.data.expr.Expr;
import platform.server.data.expr.StaticValueExpr;
import platform.server.data.query.Query;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LCP;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
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

    public Map<Object, String> fillIDs(DataSession session, LCP name, LCP classSID, Map<String, StaticCustomClass> usedSIds, Set<Integer> usedIds, Map<String, String> sidChanges, Map<Object, String> modifiedObjects) throws SQLException {
        StaticCustomClass usedClass;
        ids = new Integer[sids.length];
        Map<Object, String> modifiedNames = new HashMap<Object, String>();

        // Получаем старые sid и name
        Query<String, Object> allClassesQuery = new Query<String, Object>(Collections.singleton("key"));
        allClassesQuery.and(classSID.getExpr(session.getModifier(), BaseUtils.singleValue(allClassesQuery.mapKeys)).getWhere());
        allClassesQuery.and(BaseUtils.singleValue(allClassesQuery.mapKeys).isClass(this));
        allClassesQuery.properties.put("sid", classSID.getExpr(session.getModifier(), BaseUtils.singleValue(allClassesQuery.mapKeys)));
        allClassesQuery.properties.put("name", name.getExpr(session.getModifier(), BaseUtils.singleValue(allClassesQuery.mapKeys)));
        OrderedMap<Map<String, Object>, Map<Object, Object>> qResult = allClassesQuery.execute(session.sql, session.env);

        // Забрасываем результат запроса в map: sid -> <id, name>
        Map<String, Pair<Integer, String>> oldClasses = new HashMap<String, Pair<Integer, String>>();
        for (Map.Entry<Map<String, Object>, Map<Object, Object>> entry : qResult.entrySet()) {
            oldClasses.put(((String) entry.getValue().get("sid")).trim(),
                            new Pair<Integer, String>((Integer) BaseUtils.singleValue(entry.getKey()), ((String) entry.getValue().get("name")).trim()));
        }

        // новый sid -> старый sid
        Map<String, String> reversedChanges = BaseUtils.reverse(sidChanges);

        for(int i = 0; i < sids.length; i++) {
            String newSID = sids[i];
            if ((usedClass = usedSIds.put(newSID, this)) != null)
                throw new RuntimeException(ServerResourceBundle.getString("classes.objects.have.the.same.id", newSID, caption, usedClass.caption));

            String oldSID = newSID;
            if (reversedChanges.containsKey(newSID)) {
                oldSID = reversedChanges.get(newSID);
                modifiedObjects.put(oldClasses.get(oldSID).first, newSID);
            }

            if (oldClasses.containsKey(oldSID)) {
                if (names[i] != null && !names[i].equals(oldClasses.get(oldSID).second)) {
                    modifiedNames.put(oldClasses.get(oldSID).first, names[i]);
                }
                ids[i] = oldClasses.get(oldSID).first;
            } else {
                DataObject classObject = session.addObject(this);
                name.change(names[i], session, classObject);
                classSID.change(newSID, session, classObject);
                ids[i] = (Integer) classObject.object;
            }

            usedIds.add(ids[i]);
        }
        return modifiedNames;
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
