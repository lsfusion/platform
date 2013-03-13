package platform.server.classes;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.ObjectClassSet;
import platform.server.classes.sets.OrObjectClassSet;
import platform.server.classes.sets.UpClassSet;
import platform.server.data.expr.Expr;
import platform.server.data.expr.StaticValueExpr;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.QueryBuilder;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LCP;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class ConcreteCustomClass extends CustomClass implements ConcreteValueClass,ConcreteObjectClass, ObjectValueClassSet, StaticClass {
    public ConcreteCustomClass(String sID, String caption, CustomClass... parents) {
        super(sID, caption, parents);
    }

    public ConcreteCustomClass(String sID, String caption, List<String> sIDs, List<String> names, CustomClass... parents) {
        super(sID, caption, parents);
        assert sIDs.size() == names.size();

        for (int i = 0; i < sIDs.size(); i++) {
            staticObjectsInfo.add(new ObjectInfo(sIDs.get(i), names.get(i), null));
        }
    }

    public ConcreteCustomClass(String sID, String caption, String[] sIDs, String[] names, CustomClass... parents) {
        this(sID, caption, BaseUtils.toList(sIDs), BaseUtils.toList(names), parents);
    }

    public boolean inSet(AndClassSet set) {
        return set.containsAll(this);
    }

    public void fillNextConcreteChilds(MSet<ConcreteCustomClass> mClassSet) {
        mClassSet.add(this);
    }

    public DataObject getClassObject() {
        return new DataObject(ID, getBaseClass().objectClass);
    }

    public OrObjectClassSet getOr() {
        return new OrObjectClassSet(this);
    }

    public String getWhereString(String source) {
        return source + "=" + ID;
    }

    public String getNotWhereString(String source) {
        return source + " IS NULL OR NOT " + getWhereString(source);
    }

    public ObjectClassSet and(AndClassSet node) {
        return and(this,node);
    }

    public AndClassSet or(AndClassSet node) {
        return or(this,node); 
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean containsAll(AndClassSet node) {
        return node instanceof ConcreteCustomClass && this==node;
    }

    public ConcreteCustomClass getSingleClass() {
        if(children.isEmpty())
            return this;
        else
            return null;
    }

    // мн-ое наследование для ConcreteObjectClass
    public static ObjectClassSet and(ConcreteObjectClass set1, AndClassSet set2) {
        return set1.inSet(set2)?set1:UpClassSet.FALSE;
    }
    public static AndClassSet or(ConcreteObjectClass set1, AndClassSet set2) {
        return set1.inSet(set2)?set2:OrObjectClassSet.or(set1,set2); 
    }

    public AndClassSet getKeepClass() {
        return getBaseClass().getUpSet();
    }

    public Stat getStat() {
        return new Stat(getCount());
    }

    public AndClassSet[] getAnd() {
        return new AndClassSet[]{this};
    }

    public Integer stat = 100000;

    @Override
    public int getCount() {
        return stat;
    }

    public static class ObjectInfo {
        public ObjectInfo(String sid, String name, Integer id) {
            this.sid = sid;
            this.name = name;
            this.id = id;
        }

        public String sid;
        public String name;
        public Integer id;
    }

    private List<ObjectInfo> staticObjectsInfo = new ArrayList<ObjectInfo>();

    public boolean hasStaticObject(String sID) {
        for (ObjectInfo info : staticObjectsInfo) {
            if (info.sid.equals(sID)) {
                return true;
            }
        }
        return false;
    }

    public int getObjectID(String sID) {
        for (ObjectInfo info : staticObjectsInfo) {
            if (info.sid.equals(sID)) {
                return info.id;
            }
        }
        throw new RuntimeException("sid not found");
    }

    public String getObjectSID(int id) {
        for(ObjectInfo info : staticObjectsInfo) {
            if (info.id == id) {
                return info.sid;
            }
        }
        throw new RuntimeException("id not found");
    }

    public void addStaticObjects(List<String> sIDs, List<String> names) {
        assert sIDs.size() == names.size();
        for (int i = 0; i < sIDs.size(); i++) {
            staticObjectsInfo.add(new ObjectInfo(sIDs.get(i), names.get(i), null));
        }
    }

    public List<String> getStaticObjectsSIDs() {
        List<String> sids = new ArrayList<String>();
        for (ObjectInfo info : staticObjectsInfo) {
            sids.add(info.sid);
        }
        return sids;
    }

    public List<String> getStaticObjectsNames() {
        List<String> names = new ArrayList<String>();
        for (ObjectInfo info : staticObjectsInfo) {
            names.add(info.name);
        }
        return names;
    }

    public Map<Object, String> fillIDs(DataSession session, LCP name, LCP classSID, Map<String, ConcreteCustomClass> usedSIds, Set<Integer> usedIds, Map<String, String> sidChanges, Map<Object, String> modifiedObjects) throws SQLException {
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
        for (int i = 0, size = qResult.size(); i < size; i++) {
            ImMap<String, Object> resultKey = qResult.getKey(i);
            ImMap<String, Object> resultValue = qResult.getValue(i);
            oldClasses.put(((String) resultValue.get("sid")).trim(), new Pair<Integer, String>((Integer) resultKey.singleValue(), BaseUtils.nullTrim((String) resultValue.get("name"))));
        }

        // новый sid -> старый sid
        Map<String, String> reversedChanges = BaseUtils.reverse(sidChanges);

        for (ObjectInfo info : staticObjectsInfo) {
            String newSID = info.sid;
            ConcreteCustomClass usedClass;
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
                if (info.name != null && !info.name.equals(oldClasses.get(oldSID).second)) {
                    modifiedNames.put(oldClasses.get(oldSID).first, info.name);
                }
                info.id = oldClasses.get(oldSID).first;
            } else {
                DataObject classObject = session.addObject(this);
                name.change(info.name, session, classObject);
                classSID.change(newSID, session, classObject);
                info.id = (Integer) classObject.object;
            }

            usedIds.add(info.id);
        }
        return modifiedNames;
    }

    @Override
    public Expr getStaticExpr(Object value) {
        return new StaticValueExpr(value, this, true);
    }

    public DataObject getDataObject(String sID) {
        return new DataObject(getObjectID(sID), this);
    }

    @Override
    public String getString(Object value, SQLSyntax syntax) {
        return getDataObject((String)value).getString(syntax);
    }

}
