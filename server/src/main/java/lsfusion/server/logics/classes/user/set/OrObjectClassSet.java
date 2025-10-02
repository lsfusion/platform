package lsfusion.server.logics.classes.user.set;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.expr.join.classes.IsClassField;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.user.*;
import lsfusion.server.physics.admin.Settings;

// IMMUTABLE
public class OrObjectClassSet extends TwinImmutableObject implements OrClassSet, ObjectValueClassSet {

    public final UpClassSet up;
    public final ImSet<ConcreteCustomClass> set;
    public final boolean unknown;

    private OrObjectClassSet(UpClassSet up, ImSet<ConcreteCustomClass> set, boolean unknown) {
        this.up = up;
        this.set = set;
        this.unknown = unknown;
    }

    public OrObjectClassSet(ImSet<ConcreteCustomClass> set) {
        this(UpClassSet.FALSE, set);
    }
    public OrObjectClassSet(UpClassSet up, ImSet<ConcreteCustomClass> set) {
        this(up, set, false); 
    } 
    
    public OrObjectClassSet(UpClassSet up) {
        this(up, SetFact.EMPTY(),false);
    }

    public OrObjectClassSet(ConcreteCustomClass customClass) {
        this(UpClassSet.FALSE, SetFact.singleton(customClass),false);
    }

    public OrObjectClassSet() {
        this(UpClassSet.FALSE, SetFact.EMPTY(),true);
    }

    private OrObjectClassSet(boolean isFalse) {
        this(UpClassSet.FALSE, SetFact.EMPTY(),false);
    }
    public final static OrObjectClassSet FALSE = new OrObjectClassSet(true);

    // добавляет отфильтровывая up'ы
    private static void addAll(MSet<ConcreteCustomClass> mTo, ImSet<ConcreteCustomClass> set, UpClassSet up) {
        for(int i=0,size=set.size();i<size;i++) {
            ConcreteCustomClass nodeSet = set.get(i);
            if(up.has((CustomClass)nodeSet))
                mTo.add(nodeSet);
        }
    }

    private static boolean inSet(ImSet<ConcreteCustomClass> to, UpClassSet up,ImSet<ConcreteCustomClass> set) {
        for(int i=0,size=to.size();i<size;i++)
            if(!up.has((CustomClass)to.get(i)) && !set.contains(to.get(i))) return false;
        return true;
    }

    private static ImSet<ConcreteCustomClass> remove(ImSet<ConcreteCustomClass> to, final UpClassSet up) {
        return to.filterFn(nodeSet -> !up.has((CustomClass) nodeSet));
    }

    public OrObjectClassSet or(OrClassSet node) {
        return or((OrObjectClassSet)node);
    }

    public AndClassSet[] getAnd() {
        int size = set.size();
        AndClassSet[] result = new AndClassSet[size+(up.isEmpty()?0:1)+(unknown?1:0)]; int r=0;
        for(int i=0;i<size;i++)
            result[r++] = set.get(i);
        if(!up.isEmpty())
            result[r++] = up;
        if(unknown)
            result[r] = new OrObjectClassSet(); // бред, конечно но может и прокатит
        return result;
    }

    public OrObjectClassSet or(OrObjectClassSet node) {
        // or'им Up'ы, or'им Set'ы после чего вырезаем из Set'а все кто есть в Up'ах
        return createPack(set.merge(node.set), up.or(node.up), unknown || node.unknown);
    }

    // рекурсию нет смысло крутить, так как по сути множество не меняется, а только структурируется в меньшее с точки зрения хранения
    protected static OrObjectClassSet createPack(ImSet<ConcreteCustomClass> set, UpClassSet upSet, boolean unknown) {
        MFilterSet<ConcreteCustomClass> mRestSet = SetFact.mFilter(set);
        for(int i=0,size=set.size();i<size;i++) {
            ConcreteCustomClass element = set.get(i);
            if(element.upInSet(upSet, set))
                upSet = upSet.or(element.getUpSet());
            else
                mRestSet.keep(element);
        }
        ImSet<ConcreteCustomClass> restSet = SetFact.imFilter(mRestSet, set);
        return new OrObjectClassSet(upSet, restSet, unknown);
    }

    public OrObjectClassSet and(OrClassSet node) {
        return and((OrObjectClassSet) node);
    }

    public OrObjectClassSet and(OrObjectClassSet node) {
        // or'им Up'ы, or'им Set'ы после чего вырезаем из Set'а все кто есть в Up'ах

        MSet<ConcreteCustomClass> mAndSet = SetFact.mSet(set.filter(node.set));
        addAll(mAndSet, set, node.up);
        addAll(mAndSet, node.set, up);
        return createPack(mAndSet.immutable(), up.intersect(node.up),unknown && node.unknown);
    }
    
    public boolean isEmpty() {
        return set.isEmpty() && up.isFalse() && !unknown;
    }

    public boolean containsAll(OrClassSet node, boolean implicitCast) { // ради этого метода все и делается
        OrObjectClassSet objectNode = ((OrObjectClassSet)node);
        return !(objectNode.unknown && !unknown) && inSet(objectNode.set, up, set) && objectNode.up.inSet(up, set);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return unknown == ((OrObjectClassSet)o).unknown && up.equals(((OrObjectClassSet)o).up) && set.equals(((OrObjectClassSet)o).set);
    }

    public int immutableHashCode() {
        return 31 * (31 * up.hashCode() + set.hashCode()) + (unknown?1:0);
    }

    public String toString() {
        return set+(!up.isFalse() && !set.isEmpty()?" ":"")+(!up.isFalse()?"Up:"+ up.toString():"")+(!up.isFalse() || !set.isEmpty()?" ":"")+(unknown?"unknown":"");
    }

    // возвращает до каких классов можно дойти и с каким минимальным путем
    private static ImMap<CustomClass, Integer> recCommonClass(CustomClass customClass, ImSet<CustomClass> used, ImSet<CustomClass> commonSet, MExclMap<CustomClass, ImMap<CustomClass, Integer>> mPathes, MExclSet<CustomClass> mFirstFulls) {
        ImMap<CustomClass, Integer> cachedResult = mPathes.get(customClass);
        if(cachedResult!=null)
            return cachedResult;

        MMap<CustomClass, Integer> mChildPathes = MapFact.mMap(BaseUtils.addMinInt());
        if(commonSet.contains(customClass))
            mChildPathes.add(customClass, 0);

        boolean hasFullChild = false;
        for(CustomClass childClass : customClass.getChildrenIt())
            if(used.contains(childClass)) {
                ImMap<CustomClass, Integer> recChildPathes = recCommonClass(childClass, used, commonSet, mPathes, mFirstFulls);
                hasFullChild = hasFullChild || recChildPathes.keys().containsAll(commonSet);
                mChildPathes.addAll(recChildPathes.mapValues(value -> value + 1));
            } else
                mChildPathes.add(childClass, 1);

        ImMap<CustomClass, Integer> childPathes = mChildPathes.immutable();

        if(!hasFullChild && childPathes.keys().containsAll(commonSet))
            mFirstFulls.exclAdd(customClass);
        mPathes.exclAdd(customClass, childPathes);
        return childPathes;
    }

    @Override
    public AndClassSet getCommonAnd() {
        return this;
    }

    public CustomClass getCommonClass() {
        return getCommonClass(false);
    }

    public CustomClass getCommonClass(boolean forceConcrete) {
        assert !isEmpty();
        assert !unknown;
        ImSet<CustomClass> commonSet = SetFact.EMPTY();
        if(forceConcrete || Settings.get().isMergeUpClassSets()) {
            if(!forceConcrete && set.isEmpty() && up.getCommonClasses().length==1)
                return up.getCommonClasses()[0];

            MSet<ConcreteCustomClass> mConcrete = SetFact.mSet(set); // для детерменированности, так как upClassSet могут по разному "собираться"
            up.fillNextConcreteChilds(mConcrete); // проблема в том что свойства начинают "гулять" по таблицам
            commonSet = BaseUtils.immutableCast(mConcrete.immutable());
        }

        if(commonSet.isEmpty()) // если concrete'ов нет придется что-то брать
            commonSet = SetFact.toExclSet(up.getCommonClasses()).addExcl(set);

        return getCommonClass(commonSet);
    }

    public static CustomClass getCommonClass(final ImSet<CustomClass> commonSet) {
        if(commonSet.size()==1) // иначе firstFulls не заполнится
            return commonSet.single();

        BaseClass baseClass = commonSet.get(0).getBaseClass(); // базовая вершина

        MSet<CustomClass> mUsed = SetFact.mSet();
        for(CustomClass commonClass : commonSet) // ищем все использованные вершины
            commonClass.fillParents(mUsed);

        MExclMap<CustomClass, ImMap<CustomClass, Integer>> mPathes = MapFact.mExclMap();
        MExclSet<CustomClass> mFirstFulls = SetFact.mExclSet();
        recCommonClass(baseClass, mUsed.immutable(), commonSet, mPathes, mFirstFulls);
        final ImSet<CustomClass> firstFulls = mFirstFulls.immutable();
        ImMap<CustomClass, ImMap<CustomClass, Integer>> pathes = mPathes.immutable();

        final ImMap<CustomClass, Integer> pathCounts = pathes.mapValues((key, value) -> {
            assert !firstFulls.contains(key) || value.keys().containsAll(commonSet);
            int countCommon = 0;
            int countOthers = 0;
            for (int i = 0, size = value.size(); i < size; i++) {
                CustomClass customClass = value.getKey(i);
                if (commonSet.contains(customClass))
                    countCommon += value.getValue(i);
                else
                    countOthers += value.getValue(i);
            }
            return countOthers * 1000 + countCommon;
        });

        final MAddExclMap<CustomClass, Integer> camelCaches = MapFact.mAddExclMap();
        return firstFulls.sort((o1, o2) -> {
            int result = Integer.compare(pathCounts.get(o1), pathCounts.get(o2));
            if(result != 0)
                return result;

            result = Integer.compare(getCamelCaseCommonWords(o2, camelCaches, commonSet), getCamelCaseCommonWords(o1, camelCaches, commonSet));
            if(result != 0)
                return result;

            return o1.getCanonicalName().compareTo(o2.getCanonicalName());
        }).get(0);
    }

    private static int getCamelCaseCommonWords(CustomClass cls, MAddExclMap<CustomClass, Integer> caches, ImSet<CustomClass> commonSet) {
        Integer result = caches.get(cls);
        if(result == null) {
            result = 0;
            ImSet<String> words = SetFact.toSet(cls.getSID().split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"));
            for(CustomClass commonClass : commonSet) {
                result += words.filter(SetFact.toSet(commonClass.getSID().split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"))).size();
            }
            caches.exclAdd(cls, result);
        }
        return result;
    }

    // получает конкретный класс если он один
    public ConcreteObjectClass getSingleClass(BaseClass baseClass) {
        if(unknown) {
            if(up.isEmpty() && set.isEmpty())
                return baseClass.unknown;
        } else {
            if(!set.isEmpty()) {
                if(up.isEmpty() && set.size()==1)
                    return set.single();
            } else
                return up.getSingleClass();
        }
        return null;
    }

    public AndClassSet and(AndClassSet node) {
        return and(node.getOr());
    }

    public AndClassSet or(AndClassSet node) {
        return or(node.getOr());
    }

    public boolean containsAll(AndClassSet node, boolean implicitCast) {
        return containsAll(node.getOr(), implicitCast);
    }

    public OrObjectClassSet getOr() {
        return this;
    }

    public Type getType() {
        return ObjectType.instance;
    }
    public Stat getTypeStat(boolean forJoin) {
        if(up.isEmpty() && set.isEmpty()) {
            if(unknown)
                return Stat.MAX;
            else
                throw new RuntimeException("should not be");
        } else {
            if(up.isEmpty())
                return set.get(0).getTypeStat(forJoin);
            else
                return up.getTypeStat(forJoin);
        }
    }

    public static AndClassSet or(ObjectClassSet set1, AndClassSet set2) {
        return set1.getOr().or(set2.getOr());
    }

    private final static AddValue<Object, OrClassSet> addOr = new SymmAddValue<Object, OrClassSet>() {
        public OrClassSet addValue(Object key, OrClassSet prevValue, OrClassSet newValue) {
            return prevValue.or(newValue);
        }
    };
    public static <T> AddValue<T, OrClassSet> addOr() {
        return (AddValue<T, OrClassSet>) addOr;
    }

    // ObjectClassSet интерфейсы

    // множественное наследование
    public static int getCount(ObjectValueClassSet set) {
        long stat = 0;
        for(ConcreteCustomClass child : set.getSetConcreteChildren())
            stat += child.getCount();
        return (int)(Math.min(Integer.MAX_VALUE, stat));
    }

    public static int getClassCount(ObjectValueClassSet set) {
        return set.getSetConcreteChildren().size();
    }

    public static String getWhereString(ObjectValueClassSet set, String source) {
        ImSet<ConcreteCustomClass> children = set.getSetConcreteChildren();
        if(children.size()==0) return Where.FALSE_STRING;
        if(children.size()==1) return source + "=" + children.single().ID;
        return source + " IN (" + children.toString(value -> {
            if(value.ID == null)
                return "filling ids";
            return value.ID.toString();
        }, ",") + ")";
    }

    public static String getNotWhereString(ObjectValueClassSet set, String source) {
        return "(" + source + " IS NULL OR NOT " + getWhereString(set, source) + ")";
    }

    public long getCount() {
        assert !unknown;
        return getCount(this);
    }

    public int getClassCount() {
        return getClassCount(this);
    }

    public String getWhereString(String source) {
        return getWhereString(this, source);
    }

    public String getNotWhereString(String source) {
        return getNotWhereString(this, source);
    }

    public BaseClass getBaseClass() {
        assert !unknown;
        return (up.isEmpty() ? set.get(0) : up).getBaseClass();
    }

    public ImSet<ConcreteCustomClass> getSetConcreteChildren() {
        assert !unknown;
        return SetFact.addExclSet(up.getSetConcreteChildren(), set);
    }

    public static ObjectValueClassSet fromSetConcreteChildren(ImSet<ConcreteCustomClass> set) {
        ObjectValueClassSet result = OrObjectClassSet.FALSE;
        for(ConcreteCustomClass customClass : set)
            result = (ObjectValueClassSet) result.or(customClass);
        return result;
    }

    private static AddValue<Object, ObjectValueClassSet> objectValueSetAdd = new SymmAddValue<Object, ObjectValueClassSet>() {
        public ObjectValueClassSet addValue(Object key, ObjectValueClassSet prevValue, ObjectValueClassSet newValue) {
            return (ObjectValueClassSet) prevValue.or(newValue);
        }
    };
    public static <K> AddValue<K, ObjectValueClassSet> objectValueSetAdd() {
        return (AddValue<K, ObjectValueClassSet>) objectValueSetAdd;
    }

    public ObjectValueClassSet getValueClassSet() {
        if(!unknown) // оптимизация
            return this;
        return new OrObjectClassSet(up, set, false);
    }

    @Override
    public ResolveClassSet toResolve() {
        assert !unknown;
        ResolveUpClassSet upResolve = up.toResolve();
        if(set.isEmpty()) // оптимизация
            return upResolve;
        return new ResolveOrObjectClassSet(upResolve, set);
    }

    // множественное наследование
    public static ImRevMap<ObjectClassField, ObjectValueClassSet> getObjectClassFields(ObjectValueClassSet set) {
        return BaseUtils.immutableCast(set.getClassFields(true));
    }
    public static ImRevMap<IsClassField, ObjectValueClassSet> getIsClassFields(ObjectValueClassSet set) {
        return set.getClassFields(false);
    }

    @Override
    public boolean hasComplex() {
        assert !unknown;
        if(up.hasComplex())
            return true;

        for(ConcreteCustomClass customClass : set)
            if(customClass.hasComplex())
                return true;

        return false;
    }

    public ImRevMap<ObjectClassField, ObjectValueClassSet> getObjectClassFields() {
        return getObjectClassFields(this);
    }
    public ImRevMap<IsClassField, ObjectValueClassSet> getIsClassFields() {
        return getIsClassFields(this);
    }
    public ImRevMap<IsClassField, ObjectValueClassSet> getClassFields(boolean onlyObjectClassFields) {
        assert !unknown;
        MMap<IsClassField, ObjectValueClassSet> mMap = MapFact.mMap(up.getClassFields(onlyObjectClassFields), OrObjectClassSet.objectValueSetAdd());
        for(ConcreteCustomClass customClass : set)
            mMap.add(customClass.dataProperty, customClass);
        return CustomClass.pack(mMap.immutable().toRevExclMap(), onlyObjectClassFields, this);
    }
}
