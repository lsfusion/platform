package lsfusion.server.logics.classes.user;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFSet;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.join.classes.IsClassField;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.expr.value.StaticValueExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptedStringUtils;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ConcreteValueClass;
import lsfusion.server.logics.classes.StaticClass;
import lsfusion.server.logics.classes.user.set.*;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.navigator.controller.env.ChangesController;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.user.ClassDataProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.ImplementTable;

import java.sql.SQLException;
import java.util.*;

public class ConcreteCustomClass extends CustomClass implements ConcreteValueClass, ConcreteObjectClass, ObjectValueClassSet, StaticClass {
    public ConcreteCustomClass(String canonicalName, LocalizedString caption, String image, Version version, ImList<CustomClass> parents) {
        super(canonicalName, caption, image, version, parents);
    }

    public static void fillObjectClass(ConcreteCustomClass objectClass, List<String> names, List<LocalizedString> captions, List<String> images, Version version) {
        objectClass.addStaticObjects(names, captions, images, version);
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

    public Integer stat = null;

    @Override
    public int getCount() {
        assert ImplementTable.checkStatProps(null);
        return BaseUtils.max(stat != null ? stat : 0, 1);
    }
    
    public int getClassCount() {
        return 1;
    }

    public static class ObjectInfo {
        public ObjectInfo(String sid, String name, LocalizedString caption, String image) {
            this.sid = sid;
            this.name = name;
            this.caption = (caption != null ? caption : LocalizedString.create(name, false));
            this.image = (image != null ? image : name);
        }

        public String sid;
        public String name;
        public LocalizedString caption;
        public String image;

        public Long id;
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

    public boolean hasConcreteStaticObjects() {
        return getStaticObjectsInfoIt().iterator().hasNext();
    }
    public boolean hasStaticObject(String name) {
        for (ObjectInfo info : getStaticObjectsInfoIt()) {
            if (info.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public long getObjectID(String name) {
        for (ObjectInfo info : getStaticObjectsInfoIt()) {
            if (info.name.equals(name)) {
                return info.id;
            }
        }
        throw new RuntimeException("name not found");
    }

    public String getObjectName(long id) {
        for(ObjectInfo info : getStaticObjectsInfoIt()) {
            if (info.id == id) {
                return info.name;
            }
        }
        throw new RuntimeException("id not found");
    }

    public LocalizedString getObjectCaption(String name) {
        for(ObjectInfo info : getStaticObjectsInfoIt()) {
            if (info.name.equals(name)) {
                return info.caption;
            }
        }
        throw new RuntimeException("name not found");
    }

    public final void addStaticObjects(List<String> names, List<LocalizedString> captions, List<String> images, Version version) {
        assert names.size() == captions.size();
        for (int i = 0; i < names.size(); i++) {
            staticObjectsInfo.add(new ObjectInfo(createStaticObjectSID(names.get(i)), names.get(i), captions.get(i), images.get(i)), version);
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
    private static class PrevClass {
        public final long ID;
        public final String caption;
        public final String image;

        public PrevClass(long ID, String caption, String image) {
            this.ID = ID;
            this.caption = caption;
            this.image = image;
        }
    }

    public void fillIcons(MSet<String> mImages, ConnectionContext context) {
        String searchName = AppServerImage.Style.PROPERTY.getSearchName(context);
        for(ObjectInfo object : getStaticObjectsInfoIt())
            mImages.add(object.image + "," + searchName);
    }

    public void fillIDs(SQLSession sql, QueryEnvironment env, SQLCallable<Long> idGen, LP caption, LP image, LP name, Map<String, ConcreteCustomClass> usedSIds, Set<Long> usedIds, Map<String, String> sidChanges, DBManager.IDChanges dbChanges) throws SQLException, SQLHandledException {

        // Получаем старые sid и name
        QueryBuilder<String, String> allClassesQuery = new QueryBuilder<>(SetFact.singleton("key"));
        Expr key = allClassesQuery.getMapExprs().singleValue();
        Expr sidExpr = name.getExpr(key);
        allClassesQuery.and(sidExpr.getWhere());
        allClassesQuery.and(key.isClass(this));
        allClassesQuery.addProperty("sid", sidExpr);
        allClassesQuery.addProperty("caption", caption.getExpr(key));
        allClassesQuery.addProperty("image", image.getExpr(key));
        ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> qResult = allClassesQuery.execute(sql, env);

        // Забрасываем результат запроса в map: sid -> <id, name>
        Map<String, PrevClass> oldClasses = new HashMap<>();
        for (int i = 0, size = qResult.size(); i < size; i++) {
            ImMap<String, Object> resultKey = qResult.getKey(i);
            ImMap<String, Object> resultValue = qResult.getValue(i);
            String sid = ((String) resultValue.get("sid")).trim();
            PrevClass prevValue = oldClasses.put(sid, new PrevClass((Long) resultKey.singleValue(), BaseUtils.nullTrim((String) resultValue.get("caption")), BaseUtils.nullTrim((String) resultValue.get("image"))));
            if(prevValue != null) // temporary, CONSTRAINT on static name change, it should not happen
                dbChanges.removed.add(new DBManager.IDRemove(new DataObject(prevValue.ID, this), sid));
        }

        // new sid -> old sid
        Map<String, String> reversedChanges = BaseUtils.reverse(sidChanges);

        for (ObjectInfo info : getStaticObjectsInfoIt()) {
            String newSID = info.sid; // todo [dale]: Тут (и вообще при синхронизации) мы используем SID (с подчеркиванием), хотя, наверное, можно уже переходить на канонические имена 
            ConcreteCustomClass usedClass;
            if ((usedClass = usedSIds.put(newSID, this)) != null)
                throw new RuntimeException(ThreadLocalContext.localize(LocalizedString.createFormatted("{classes.objects.have.the.same.id}", newSID, this.caption, usedClass.caption)));

            PrevClass oldObject;
            if (reversedChanges.containsKey(newSID)) {
                oldObject = oldClasses.remove(reversedChanges.get(newSID));
                if (oldObject != null) {
                    dbChanges.modifiedSIDs.put(new DataObject(oldObject.ID, this), newSID);
                }
            } else
                oldObject = oldClasses.remove(newSID);

            String staticObjectCaption = ThreadLocalContext.localize(info.caption);
            String staticObjectImage = ScriptedStringUtils.wrapImage(info.image);
            if (oldObject != null) {
                if (!staticObjectCaption.equals(oldObject.caption)) {
                    dbChanges.modifiedCaptions.put(new DataObject(oldObject.ID, this), staticObjectCaption);
                }
                if (!BaseUtils.nullEquals(staticObjectImage, oldObject.image)) {
                    dbChanges.modifiedImages.put(new DataObject(oldObject.ID, this), staticObjectImage);
                }
                info.id = oldObject.ID;
            } else {
                Long id = idGen.call();
                dbChanges.added.add(new DBManager.IDAdd(id, this, newSID, staticObjectCaption, staticObjectImage));
                info.id = id;
            }

            usedIds.add(info.id);
        }
        for(Map.Entry<String, PrevClass> oldClass : oldClasses.entrySet())
            if(!ID.equals(oldClass.getValue().ID)) // need this for objectClass
                dbChanges.removed.add(new DBManager.IDRemove(new DataObject(oldClass.getValue().ID, this), oldClass.getKey()));
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

    public ImSet<ConcreteCustomClass> getSetConcreteChildren() {
        return SetFact.singleton(this);
    }

    public ClassDataProperty dataProperty;
    public Long readData(Long data, SQLSession sql, QueryEnvironment env, ChangesController changesController) throws SQLException, SQLHandledException {
        return (Long) dataProperty.read(sql, MapFact.singleton(dataProperty.interfaces.single(), new DataObject(data, this)), Property.defaultModifier, env, changesController);
    }

    public ImRevMap<ObjectClassField, ObjectValueClassSet> getObjectClassFields() {
        return OrObjectClassSet.getObjectClassFields(this);
    }

    public ImRevMap<IsClassField, ObjectValueClassSet> getIsClassFields() {
        return OrObjectClassSet.getIsClassFields(this);
    }

    public ImRevMap<IsClassField, ObjectValueClassSet> getClassFields(boolean onlyObjectClassFields) {
        return MapFact.singletonRev(dataProperty, this);
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

    public ImSet<Property> getChangeProps(ConcreteObjectClass cls) {
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

    public void updateStat(ImMap<Long, Integer> classStats) {
        stat = classStats.get(ID);
    }
    public void updateSIDStat(ImMap<String, Integer> classStats) {
        assert ID == null;
        stat = classStats.get(getSID());
    }
}
