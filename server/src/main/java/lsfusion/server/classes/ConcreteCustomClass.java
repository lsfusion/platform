package lsfusion.server.classes;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.ObjectClassSet;
import lsfusion.server.classes.sets.OrObjectClassSet;
import lsfusion.server.classes.sets.UpClassSet;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.StaticValueExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassDataProperty;
import lsfusion.server.logics.property.ClassField;
import lsfusion.server.logics.property.Property;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class ConcreteCustomClass extends CustomClass implements ConcreteValueClass,ConcreteObjectClass, ObjectValueClassSet, StaticClass {
    public ConcreteCustomClass(String sID, String caption, CustomClass... parents) {
        super(sID, caption, parents);
    }

    public ConcreteCustomClass(String sID, String caption, List<String> names, List<String> captions, CustomClass... parents) {
        super(sID, caption, parents);
        assert names.size() == captions.size();
        addStaticObjects(names, captions);
    }

    public static ConcreteCustomClass createObjectClass(String sID, String caption, List<String> names, List<String> captions, CustomClass... parents) {
        ConcreteCustomClass objectClass = new ConcreteCustomClass(sID, caption, names, captions, parents);
        for (int i = 0; i < objectClass.staticObjectsInfo.size(); i++) {
            objectClass.staticObjectsInfo.get(i).sid = objectClass.staticObjectsInfo.get(i).name;
        }
        return objectClass;
    }

    public static boolean inSet(ConcreteClass cClass, AndClassSet set) {
        return set.containsAll(cClass, true); // считаем что примитивы могут быть implicit cast'ся
    }
    public boolean inSet(AndClassSet set) {
        return inSet(this, set);
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
        return OrObjectClassSet.getWhereString(this, source);
    }

    public String getNotWhereString(String source) {
        return OrObjectClassSet.getNotWhereString(this, source);
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

    public boolean containsAll(AndClassSet node, boolean implicitCast) {
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

    public AndClassSet[] getAnd() {
        return new AndClassSet[]{this};
    }

    public Integer stat = 100000;

    @Override
    public int getCount() {
        return stat;
    }

    public int getClassCount() {
        return 1;
    }

    public static class ObjectInfo {
        public ObjectInfo(String sid, String name, String caption, Integer id) {
            this.sid = sid;
            this.name = name;
            this.caption = caption.trim();
            this.id = id;
        }

        public String sid;
        public String name;
        public String caption;
        public Integer id;
    }

    private List<ObjectInfo> staticObjectsInfo = new ArrayList<ObjectInfo>();

    public boolean hasStaticObject(String name) {
        for (ObjectInfo info : staticObjectsInfo) {
            if (info.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public int getObjectID(String name) {
        for (ObjectInfo info : staticObjectsInfo) {
            if (info.name.equals(name)) {
                return info.id;
            }
        }
        throw new RuntimeException("name not found");
    }

    public String getObjectName(int id) {
        for(ObjectInfo info : staticObjectsInfo) {
            if (info.id == id) {
                return info.name;
            }
        }
        throw new RuntimeException("id not found");
    }

    public final void addStaticObjects(List<String> names, List<String> captions) {
        assert names.size() == captions.size();
        for (int i = 0; i < names.size(); i++) {
            staticObjectsInfo.add(new ObjectInfo(createStaticObjectSID(names.get(i)), names.get(i), captions.get(i), null));
        }
    }

    public List<String> getStaticObjectsNames() {
        List<String> sids = new ArrayList<String>();
        for (ObjectInfo info : staticObjectsInfo) {
            sids.add(info.name);
        }
        return sids;
    }

    public List<String> getStaticObjectsCaptions() {
        List<String> names = new ArrayList<String>();
        for (ObjectInfo info : staticObjectsInfo) {
            names.add(info.caption);
        }
        return names;
    }

    public Map<DataObject, String> fillIDs(DataSession session, LCP name, LCP staticName, Map<String, ConcreteCustomClass> usedSIds, Set<Integer> usedIds, Map<String, String> sidChanges, Map<DataObject, String> modifiedObjects) throws SQLException, SQLHandledException {
        Map<DataObject, String> modifiedNames = new HashMap<DataObject, String>();

        // Получаем старые sid и name
        QueryBuilder<String, String> allClassesQuery = new QueryBuilder<String, String>(SetFact.singleton("key"));
        Expr key = allClassesQuery.getMapExprs().singleValue();
        Expr sidExpr = staticName.getExpr(session.getModifier(), key);
        allClassesQuery.and(sidExpr.getWhere());
        allClassesQuery.and(key.isClass(this));
        allClassesQuery.addProperty("sid", sidExpr);
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
            String newSID = info.sid; // todo [dale]:
            ConcreteCustomClass usedClass;
            if ((usedClass = usedSIds.put(newSID, this)) != null)
                throw new RuntimeException(ServerResourceBundle.getString("classes.objects.have.the.same.id", newSID, caption, usedClass.caption));

            String oldSID = newSID;
            if (reversedChanges.containsKey(newSID)) {
                oldSID = reversedChanges.get(newSID);
                if (oldClasses.containsKey(oldSID)) {
                    modifiedObjects.put(new DataObject(oldClasses.get(oldSID).first, this), newSID);
                }
            }

            if (oldClasses.containsKey(oldSID)) {
                if (info.caption != null && !info.caption.equals(oldClasses.get(oldSID).second)) {
                    modifiedNames.put(new DataObject(oldClasses.get(oldSID).first, this), info.caption);
                }
                info.id = oldClasses.get(oldSID).first;
            } else {
                DataObject classObject = session.addObject(this);
                name.change(info.caption, session, classObject);
                staticName.change(newSID, session, classObject);
                info.id = (Integer) classObject.object;
            }

            usedIds.add(info.id);
        }
        return modifiedNames;
    }

    private String createStaticObjectSID(String objectName) {
        return getSID() + "." + objectName;
    }

    @Override
    public Expr getStaticExpr(Object value) {
        return new StaticValueExpr(value, this, true);
    }

    public DataObject getDataObject(String name) {
        return new DataObject(getObjectID(name), this);
    }

    @Override
    public String getString(Object value, SQLSyntax syntax) {
        return getDataObject((String)value).getString(syntax);
    }

    public ImSet<ConcreteCustomClass> getSetConcreteChildren() {
        return SetFact.singleton(this);
    }

    public ClassDataProperty dataProperty;
    public Integer readData(Integer data, SQLSession sql) throws SQLException, SQLHandledException {
        return (Integer) dataProperty.read(sql, MapFact.singleton(dataProperty.interfaces.single(), new DataObject(data, this)), Property.defaultModifier, QueryEnvironment.empty);
    }

    public ImRevMap<ClassField, ObjectValueClassSet> getTables() {
        return MapFact.singletonRev((ClassField)dataProperty, (ObjectValueClassSet)this);
    }

    public ValueClassSet getValueClassSet() {
        return this;
    }

    public void getDiffSet(ConcreteObjectClass diffClass, MSet<CustomClass> mAddClasses, MSet<CustomClass> mRemoveClasses) {
        if(diffClass instanceof UnknownClass) { // если неизвестный то все добавляем
            fillParents(mAddClasses);
            return;
        }

        MAddMap<CustomClass, Check> checks = commonClassSet1(true); // check
        if(diffClass!=null) ((CustomClass)diffClass).commonClassSet2(false,mRemoveClasses,true,checks);

        commonClassSet3(null,mAddClasses,true,checks);
    }

    public ImSet<CalcProperty> getChangeProps(ConcreteObjectClass cls) {
        MSet<CustomClass> mAddClasses = SetFact.mSet();
        MSet<CustomClass> mRemoveClasses = SetFact.mSet();
        cls.getDiffSet(this, mAddClasses, mRemoveClasses);
        return getChangeProperties(mAddClasses.immutable(), mRemoveClasses.immutable());
    }

}
