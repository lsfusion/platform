package lsfusion.server.classes;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.classes.sets.*;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.StaticValueExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.i18n.FormatLocalizedString;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFSet;
import lsfusion.server.logics.property.*;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class ConcreteCustomClass extends CustomClass implements ConcreteValueClass,ConcreteObjectClass, ObjectValueClassSet, StaticClass {
    public ConcreteCustomClass(String sID, LocalizedString caption, Version version, CustomClass... parents) {
        super(sID, caption, version, parents);
    }

    public static void fillObjectClass(ConcreteCustomClass objectClass, List<String> names, List<LocalizedString> captions, Version version) {
        objectClass.addStaticObjects(names, captions, version);
        for (ObjectInfo info : objectClass.getNFStaticObjectsInfoIt(version))
            info.sid = info.name;
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
        if(node instanceof ConcreteClass)
            return this == node;
        if(node instanceof UpClassSet) // без этих проверок будет минимум assert false в CustomClass.pack валится
            return ((UpClassSet)node).inSet(UpClassSet.FALSE, SetFact.singleton(this));
        return getOr().containsAll((OrClassSet) node, implicitCast);
    }

    public ConcreteCustomClass getSingleClass() {
        if(getChildren().isEmpty())
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

    public Integer stat = 1000;

    @Override
    public int getCount() {
        return stat;
    }

    public int getClassCount() {
        return 1;
    }

    public static class ObjectInfo {
        public ObjectInfo(String sid, String name, LocalizedString caption, Integer id) {
            this.sid = sid;
            this.name = name;
            this.caption = caption;
            this.id = id;
        }

        public String sid;
        public String name;
        public LocalizedString caption;
        public Integer id;
    }

    private NFSet<ObjectInfo> staticObjectsInfo = NFFact.set();
    public Iterable<ObjectInfo> getStaticObjectsInfoIt() {
        return staticObjectsInfo.getIt();
    }
    public Iterable<ObjectInfo> getNFStaticObjectsInfoIt(Version version) {
        return staticObjectsInfo.getNFIt(version);
    }

    public boolean hasNFStaticObject(String name, Version version) {
        for (ObjectInfo info : getNFStaticObjectsInfoIt(version)) {
            if (info.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasStaticObject(String name) {
        for (ObjectInfo info : getStaticObjectsInfoIt()) {
            if (info.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public int getObjectID(String name) {
        for (ObjectInfo info : getStaticObjectsInfoIt()) {
            if (info.name.equals(name)) {
                return info.id;
            }
        }
        throw new RuntimeException("name not found");
    }

    public String getObjectName(int id) {
        for(ObjectInfo info : getStaticObjectsInfoIt()) {
            if (info.id == id) {
                return info.name;
            }
        }
        throw new RuntimeException("id not found");
    }

    public final void addStaticObjects(List<String> names, List<LocalizedString> captions, Version version) {
        assert names.size() == captions.size();
        for (int i = 0; i < names.size(); i++) {
            staticObjectsInfo.add(new ObjectInfo(createStaticObjectSID(names.get(i)), names.get(i), captions.get(i), null), version);
        }
    }

    public List<String> getNFStaticObjectsNames(Version version) {
        List<String> sids = new ArrayList<>();
        for (ObjectInfo info : getNFStaticObjectsInfoIt(version)) {
            sids.add(info.name);
        }
        return sids;
    }

    public List<LocalizedString> getNFStaticObjectsCaptions(Version version) {
        List<LocalizedString> names = new ArrayList<>();
        for (ObjectInfo info : getNFStaticObjectsInfoIt(version)) {
            names.add(info.caption);
        }
        return names;
    }

    public Map<DataObject, String> fillIDs(DataSession session, LCP name, LCP staticName, Map<String, ConcreteCustomClass> usedSIds, Set<Integer> usedIds, Map<String, String> sidChanges, Map<DataObject, String> modifiedObjects) throws SQLException, SQLHandledException {
        Map<DataObject, String> modifiedNames = new HashMap<>();

        // Получаем старые sid и name
        QueryBuilder<String, String> allClassesQuery = new QueryBuilder<>(SetFact.singleton("key"));
        Expr key = allClassesQuery.getMapExprs().singleValue();
        Expr sidExpr = staticName.getExpr(session.getModifier(), key);
        allClassesQuery.and(sidExpr.getWhere());
        allClassesQuery.and(key.isClass(this));
        allClassesQuery.addProperty("sid", sidExpr);
        allClassesQuery.addProperty("name", name.getExpr(session.getModifier(), key));
        ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> qResult = allClassesQuery.execute(session.sql, session.env);

        // Забрасываем результат запроса в map: sid -> <id, name>
        Map<String, Pair<Integer, String>> oldClasses = new HashMap<>();
        for (int i = 0, size = qResult.size(); i < size; i++) {
            ImMap<String, Object> resultKey = qResult.getKey(i);
            ImMap<String, Object> resultValue = qResult.getValue(i);
            oldClasses.put(((String) resultValue.get("sid")).trim(), new Pair<>((Integer) resultKey.singleValue(), BaseUtils.nullTrim((String) resultValue.get("name"))));
        }

        // новый sid -> старый sid
        Map<String, String> reversedChanges = BaseUtils.reverse(sidChanges);

        for (ObjectInfo info : getStaticObjectsInfoIt()) {
            String newSID = info.sid; // todo [dale]:
            ConcreteCustomClass usedClass;
            if ((usedClass = usedSIds.put(newSID, this)) != null)
                throw new RuntimeException(ThreadLocalContext.localize(new FormatLocalizedString("{classes.objects.have.the.same.id}", newSID, caption, usedClass.caption)));

            String oldSID = newSID;
            if (reversedChanges.containsKey(newSID)) {
                oldSID = reversedChanges.get(newSID);
                if (oldClasses.containsKey(oldSID)) {
                    modifiedObjects.put(new DataObject(oldClasses.get(oldSID).first, this), newSID);
                }
            }

            if (oldClasses.containsKey(oldSID)) {
                if (info.caption != null && !info.caption.getSourceString().equals(oldClasses.get(oldSID).second)) {
                    modifiedNames.put(new DataObject(oldClasses.get(oldSID).first, this), info.caption.getSourceString());
                }
                info.id = oldClasses.get(oldSID).first;
            } else {
                DataObject classObject = session.addObject(this);
                name.change(info.caption.getSourceString(), session, classObject);
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
        return (Integer) dataProperty.read(sql, MapFact.singleton(dataProperty.interfaces.single(), new DataObject(data, this)), Property.defaultModifier, DataSession.emptyEnv(OperationOwner.unknown));
    }

    public ImRevMap<ObjectClassField, ObjectValueClassSet> getObjectClassFields() {
        return OrObjectClassSet.getObjectClassFields(this);
    }

    public ImRevMap<IsClassField, ObjectValueClassSet> getIsClassFields() {
        return OrObjectClassSet.getIsClassFields(this);
    }

    public ImRevMap<IsClassField, ObjectValueClassSet> getClassFields(boolean onlyObjectClassFields) {
        return MapFact.singletonRev((IsClassField)dataProperty, (ObjectValueClassSet)this);
    }

    public ObjectValueClassSet getValueClassSet() {
        return this;
    }

    public void getDiffSet(ConcreteObjectClass diffClass, MSet<CustomClass> mAddClasses, MSet<CustomClass> mRemoveClasses) {
        if(diffClass instanceof UnknownClass) { // если неизвестный то все добавляем
            fillParents(mAddClasses);
            return;
        }
        
        BaseUtils.fillDiffChildren(this, getParents, (CustomClass)diffClass, mAddClasses, mRemoveClasses);
    }

    public ImSet<CalcProperty> getChangeProps(ConcreteObjectClass cls) {
        MSet<CustomClass> mAddClasses = SetFact.mSet();
        MSet<CustomClass> mRemoveClasses = SetFact.mSet();
        cls.getDiffSet(this, mAddClasses, mRemoveClasses);
        return getChangeProperties(mAddClasses.immutable(), mRemoveClasses.immutable());
    }

    public ResolveClassSet toResolve() {
        return new ResolveOrObjectClassSet(ResolveUpClassSet.FALSE, SetFact.singleton(this));
    }

    @Override
    public String getShortName() {
        return ThreadLocalContext.localize(getCaption());
    }

    public boolean isZero(Object object) {
        return false;
    }
}
