package platform.server.classes;

import platform.base.ArrayInstancer;
import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.StaticValueExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LCP;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StaticCustomClass extends ConcreteCustomClass implements StaticClass {

    private String[] sids;
    private String[] names;
    private Integer[] ids;

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
        QueryBuilder<String, String> allClassesQuery = new QueryBuilder<String, String>(SetFact.singleton("key"));
        Expr key = allClassesQuery.getMapExprs().singleValue();
        allClassesQuery.and(classSID.getExpr(session.getModifier(), key).getWhere());
        allClassesQuery.and(key.isClass(this));
        allClassesQuery.addProperty("sid", classSID.getExpr(session.getModifier(), key));
        allClassesQuery.addProperty("name", name.getExpr(session.getModifier(), key));
        ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> qResult = allClassesQuery.execute(session.sql, session.env);

        // Забрасываем результат запроса в map: sid -> <id, name>
        Map<String, Pair<Integer, String>> oldClasses = new HashMap<String, Pair<Integer, String>>();
        for (int i=0,size=qResult.size();i<size;i++) {
            ImMap<String, Object> resultKey = qResult.getKey(i); ImMap<String, Object> resultValue = qResult.getValue(i);
            oldClasses.put(((String) resultValue.get("sid")).trim(),
                            new Pair<Integer, String>((Integer) resultKey.singleValue(), BaseUtils.nullTrim((String) resultValue.get("name"))));
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
                if (oldClasses.containsKey(oldSID)) {
                    modifiedObjects.put(oldClasses.get(oldSID).first, newSID);
                }
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

    public void changeInstances(String[] sids, String[] names) {
        assert ids == null;
        this.sids = sids;
        this.names = names;
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

    public String[] getSids() {
        return sids;
    }

    public String[] getNames() {
        return names;
    }
}
