package lsfusion.server.classes.sets;

import lsfusion.base.BaseUtils;
import lsfusion.base.SFunctionSet;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.Settings;
import lsfusion.server.classes.*;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.property.ClassField;

import java.util.Comparator;

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

    public OrObjectClassSet(UpClassSet up) {
        this(up, SetFact.<ConcreteCustomClass>EMPTY(),false);
    }

    public OrObjectClassSet(ConcreteCustomClass customClass) {
        this(UpClassSet.FALSE, SetFact.singleton(customClass),false);
    }

    public OrObjectClassSet() {
        this(UpClassSet.FALSE, SetFact.<ConcreteCustomClass>EMPTY(),true);
    }

    private OrObjectClassSet(boolean isFalse) {
        this(UpClassSet.FALSE, SetFact.<ConcreteCustomClass>EMPTY(),false);
    }
    public final static OrObjectClassSet FALSE = new OrObjectClassSet(true);

    // добавляет отфильтровывая up'ы
    private static void addAll(MSet<ConcreteCustomClass> mTo, ImSet<ConcreteCustomClass> set, UpClassSet up) {
        for(int i=0,size=set.size();i<size;i++) {
            ConcreteCustomClass nodeSet = set.get(i);
            if(up.has(nodeSet))
                mTo.add(nodeSet);
        }
    }

    private static boolean inSet(ImSet<ConcreteCustomClass> to, UpClassSet up,ImSet<ConcreteCustomClass> set) {
        for(int i=0,size=to.size();i<size;i++)
            if(!up.has(to.get(i)) && !set.contains(to.get(i))) return false;
        return true;
    }

    private static ImSet<ConcreteCustomClass> remove(ImSet<ConcreteCustomClass> to, final UpClassSet up) {
        return to.filterFn(new SFunctionSet<ConcreteCustomClass>() {
            public boolean contains(ConcreteCustomClass nodeSet) {
                return !up.has(nodeSet);
            }
        });
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
                upSet = upSet.or(new UpClassSet(element));
            else
                mRestSet.keep(element);
        }
        ImSet<ConcreteCustomClass> restSet = SetFact.imFilter(mRestSet, set);
        return new OrObjectClassSet(upSet, restSet, unknown);
    }

    public OrObjectClassSet and(OrClassSet node) {
        return and((OrObjectClassSet)node);
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

    public boolean twins(TwinImmutableObject o) {
        return unknown == ((OrObjectClassSet)o).unknown && up.equals(((OrObjectClassSet)o).up) && set.equals(((OrObjectClassSet)o).set);
    }

    @Override
    public int immutableHashCode() {
        return 31 * (31 * up.hashCode() + set.hashCode()) + (unknown?1:0);
    }

    public int hashCode() {
        return 1;
    }

    public String toString() {
        return set+(!up.isFalse() && !set.isEmpty()?" ":"")+(!up.isFalse()?"Up:"+ up.toString():"")+(!up.isFalse() || !set.isEmpty()?" ":"")+(unknown?"unknown":"");
    }

    // возвращает до каких путей можно дойти и с каким минимальным путем
    private static ImMap<CustomClass, Integer> recCommonClass(CustomClass customClass, ImSet<CustomClass> used, ImSet<CustomClass> commonSet, MExclMap<CustomClass, ImMap<CustomClass, Integer>> mPathes, MExclSet<CustomClass> mFirstFulls) {
        ImMap<CustomClass, Integer> cachedResult = mPathes.get(customClass);
        if(cachedResult!=null)
            return cachedResult;

        MMap<CustomClass, Integer> mChildPathes = MapFact.mMap(BaseUtils.<CustomClass>addMinInt());
        if(commonSet.contains(customClass))
            mChildPathes.add(customClass, 0);

        boolean hasFullChild = false;
        for(CustomClass childClass : customClass.children)
            if(used.contains(childClass)) {
                ImMap<CustomClass, Integer> recChildPathes = recCommonClass(childClass, used, commonSet, mPathes, mFirstFulls);
                hasFullChild = hasFullChild || recChildPathes.keys().containsAll(commonSet);
                mChildPathes.addAll(recChildPathes.mapValues(new GetValue<Integer, Integer>() {
                    public Integer getMapValue(Integer value) {
                        return value + 1;
                    }
                }));
            } else
                mChildPathes.add(childClass, 1);

        ImMap<CustomClass, Integer> childPathes = mChildPathes.immutable();

        if(!hasFullChild && childPathes.keys().containsAll(commonSet))
            mFirstFulls.exclAdd(customClass);
        mPathes.exclAdd(customClass, childPathes);
        return childPathes;
    }

    public CustomClass getCommonClass() {
        assert !isEmpty();
        assert !unknown;

        final ImSet<CustomClass> commonSet;
        if(Settings.get().isMergeUpClassSets()) {
            if(set.isEmpty() && up.getCommonClasses().length==1)
                return up.getCommonClasses()[0];

            MSet<ConcreteCustomClass> mConcrete = SetFact.mSet(set); // для детерменированности, так как upClassSet могут по разному "собираться"
            up.fillNextConcreteChilds(mConcrete);
            commonSet = BaseUtils.immutableCast(mConcrete.immutable());
        } else
            commonSet = SetFact.toExclSet(up.getCommonClasses()).addExcl(set);

        if(commonSet.size()==1) // иначе firstFulls не заполнится
            return commonSet.single();

        BaseClass baseClass = getBaseClass(); // базовая вершина

        MSet<CustomClass> mUsed = SetFact.mSet();
        for(CustomClass commonClass : commonSet) // ищем все использованные вершины
            commonClass.fillParents(mUsed);

        MExclMap<CustomClass, ImMap<CustomClass, Integer>> mPathes = MapFact.mExclMap();
        MExclSet<CustomClass> mFirstFulls = SetFact.mExclSet();
        recCommonClass(baseClass, mUsed.immutable(), commonSet, mPathes, mFirstFulls);
        final ImSet<CustomClass> firstFulls = mFirstFulls.immutable();
        ImMap<CustomClass, ImMap<CustomClass, Integer>> pathes = mPathes.immutable();

        final ImMap<CustomClass, Integer> pathCounts = pathes.mapValues(new GetKeyValue<Integer, CustomClass, ImMap<CustomClass, Integer>>() {
            public Integer getMapValue(CustomClass key, ImMap<CustomClass, Integer> value) {
                assert !firstFulls.contains(key) || value.keys().containsAll(commonSet);
                int countCommon = 0;
                int countOthers = 0;
                for (int i = 0, size = value.size(); i < size; i++) {
                    CustomClass customClass = value.getKey(i);
                    if(commonSet.contains(customClass))
                        countCommon += value.getValue(i);
                    else
                        countOthers += value.getValue(i);
                }
                return countOthers * 1000 + countCommon;
            }
        });

        return firstFulls.sort(new Comparator<CustomClass>() {
            public int compare(CustomClass o1, CustomClass o2) {
                int cnt1 = pathCounts.get(o1);
                int cnt2 = pathCounts.get(o2);
                if (cnt1 > cnt2)
                    return 1;
                if (cnt1 < cnt2)
                    return -1;
                return o1.getSID().compareTo(o2.getSID());
            }
        }).get(0);
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
    public Stat getTypeStat() {
        if(up.isEmpty() && set.isEmpty()) {
            if(unknown)
                return Stat.MAX;
            else
                throw new RuntimeException("should not be");
        } else {
            if(up.isEmpty())
                return set.get(0).getTypeStat();
            else
                return up.getTypeStat();
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
        int stat = 0;
        for(ConcreteCustomClass child : set.getSetConcreteChildren())
            stat += child.getCount();
        return stat;
    }

    public static int getClassCount(ObjectValueClassSet set) {
        return set.getSetConcreteChildren().size();
    }

    public static String getWhereString(ObjectValueClassSet set, String source) {
        ImSet<ConcreteCustomClass> children = set.getSetConcreteChildren();
        if(children.size()==0) return Where.FALSE_STRING;
        if(children.size()==1) return source + "=" + children.single().ID;
        return source + " IN (" + children.toString(new GetValue<String, ConcreteCustomClass>() {
            public String getMapValue(ConcreteCustomClass value) {
                return value.ID.toString();
            }
        }, ",") + ")";
    }

    public static String getNotWhereString(ObjectValueClassSet set, String source) {
        return "(" + source + " IS NULL OR NOT " + getWhereString(set, source) + ")";
    }

    public int getCount() {
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
    public ImRevMap<ClassField, ObjectValueClassSet> getTables() {
        assert !unknown;
        MMap<ClassField, ObjectValueClassSet> mMap = MapFact.mMap(up.getTables(), OrObjectClassSet.<ClassField>objectValueSetAdd());
        for(ConcreteCustomClass customClass : set)
            mMap.add(customClass.dataProperty, customClass);
        return mMap.immutable().toRevExclMap();
    }

    public ValueClassSet getValueClassSet() {
        if(!unknown) // оптимизация
            return this;
        return new OrObjectClassSet(up, set, false);
    }
}
