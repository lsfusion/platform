package platform.server.data.where.classes;

import platform.base.ArrayCombinations;
import platform.base.ArrayInstancer;
import platform.base.BaseUtils;
import platform.base.ExtraMapSetWhere;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.WrapMap;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.AddValue;
import platform.base.col.interfaces.mutable.MMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.SimpleAddValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.ManualLazy;
import platform.server.classes.ValueClass;
import platform.server.classes.ValueClassSet;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.OrClassSet;
import platform.server.classes.sets.OrObjectClassSet;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;


// !!! equals'ы и hashCode должны только в meanWheres вызываться
// предполагается ДНФ потому как в отличии от КНФ у Set'ов пустое пересечение актуально в данной задаче в то время как универсального мн-ва здесь нету 
public abstract class AbstractClassWhere<K, This extends AbstractClassWhere<K, This>> extends ExtraMapSetWhere<K,AndClassSet,AbstractClassWhere.And<K>,This> {

    public This or(This where) {
        return add(where);
    }
    public This and(This where) {
        return intersect(where);
    }
    
    public static abstract class NF<K, V> extends WrapMap<K, V> {

        protected NF(ImMap<? extends K, ? extends V> map) {
            super(map);
        }

        protected NF(K key, V value) {
            super(key, value);
        }

        protected abstract boolean containsAll(V who, V what);

        public boolean containsAll(NF<K, V> set) {
            if (size() > set.size()) return false; // если больше то содержать не может

            for (int i = 0, size = size() ; i < size; i++) {
                V inSet = set.get(getKey(i));
                if (inSet == null || !(containsAll(getValue(i), inSet))) return false;
            }
            return true;
        }
    }

    private final static AddValue<Object, AndClassSet> addAnd = new AddValue<Object, AndClassSet>() {
        public AndClassSet addValue(Object key, AndClassSet prevValue, AndClassSet newValue) {
            AndClassSet andValue = prevValue.and(newValue);
            if(andValue.isEmpty())
                return null;
            return andValue;
        }

        public boolean symmetric() {
            return true;
        }

        public boolean stopWhenNull() {
            return true;
        }
    };
    public static <K> AddValue<K, AndClassSet> addAnd() {
        return (AddValue<K, AndClassSet>) addAnd;
    }

    private final static AddValue<Object, AndClassSet> addOr = new SimpleAddValue<Object, AndClassSet>() {
        public AndClassSet addValue(Object key, AndClassSet prevValue, AndClassSet newValue) {
            return prevValue.or(newValue);
        }

        public boolean symmetric() {
            return true;
        }
    };
    public static <K> AddValue<K, AndClassSet> addOr() {
        return (AddValue<K, AndClassSet>) addAnd;
    }

    public static class And<K> extends NF<K, AndClassSet> {

        public And(ImMap<? extends K, ? extends AndClassSet> map) {
            super(map);
        }

        public And(K key, AndClassSet value) {
            super(key, value);
        }

        private final static And empty = new And(MapFact.EMPTY());
        public static <K> And<K> EMPTY() {
            return empty;
        }

        public boolean compatible(And<K> and) {
            for (int i = 0, size = size(); i < size; i++) {
                AndClassSet andClassSet = and.get(getKey(i));
                if (andClassSet == null || getValue(i).getType().getCompatible(andClassSet.getType()) == null) {
                    return false;
                }
            }
            return true;
        }

        protected And<K> intersect(And<K> where2) {
            ImMap<K, AndClassSet> result = map.merge(where2.map, AbstractClassWhere.<K>addAnd()); // так быстрее этот участок кода выполняется, ОЧЕНЬ много раз
            if(result==null)
                return null;
            return new And<K>(result);
        }

        public <T> And<T> remap(ImRevMap<K, ? extends T> remap) {
            return new And<T>((ImMap<T,AndClassSet>) remap.rightCrossJoin(map));
        }
        public <T> And<T> innerRemap(ImRevMap<K, ? extends T> remap) {
            return new And<T>((ImMap<T,AndClassSet>) remap.innerCrossJoin(map));
        }
        public <T> And<T> mapBack(ImRevMap<T, ? extends K> remap) {
            return new And<T>(((ImRevMap<T, K>)remap).join(map));
        }

        public And<K> remove(ImSet<? extends K> keys) {
            return new And<K>(map.remove(keys));
        }

        public <T extends K> And<T> filterKeys(ImSet<T> keys) {
            return new And<T>(map.filter(keys));
        }

        public <T extends K> And<T> filterInclKeys(ImSet<T> keys) {
            return new And<T>(map.filterIncl(keys));
        }

        protected boolean containsAll(AndClassSet who, AndClassSet what) {
            return who.containsAll(what);
        }

        public Where getWhere(GetValue<Expr, K> mapExprs) {
            Where result = Where.TRUE;
            for(int i=0,size=size();i<size;i++) {
                AndClassSet value = getValue(i);
                if(BaseUtils.hashEquals(value, value.getValueClassSet())) // если ValueClassSet, тут формально можно добавлять and Not BaseClass
                    result = result.and(mapExprs.getMapValue(getKey(i)).isClass((ValueClassSet)value));
            }
            return result;
        }

        public And<K>[] andNot(AbstractClassWhere<K, ?> where) {
            ImOrderMap<K,AndClassSet> orderAnd = toOrderMap();

            AndClassSet[][] ands = new AndClassSet[orderAnd.size()][];
            for(int i=0;i<orderAnd.size();i++)
                ands[i] = orderAnd.getValue(i).getAnd();
            ArrayCombinations<AndClassSet> combs = new ArrayCombinations<AndClassSet>(ands, AndClassSet.arrayInstancer);
            if(combs.max==1) {
                if(where.meansFrom(this))
                    return new And[0];
                else
                    return null; // не меняем ничего
            }
            And<K>[] keep = new And[combs.max]; int k=0;
            for(AndClassSet[] comb : combs) {
                And<K> and = new And<K>(orderAnd.replaceValues(comb).getMap());
                if(!where.meansFrom(and))
                    keep[k++] = and;
            }
            if(combs.max==k)
                return null;
            And<K>[] result = new And[k]; System.arraycopy(keep, 0, result, 0, k);
            return result;
        }
    }

    protected And<K> createMap(ImMap<K, AndClassSet> map) {
        return new And<K>(map);
    }

    protected AndClassSet addMapValue(AndClassSet value1, AndClassSet value2) {
        return value1.or(value2);
    }

    protected AbstractClassWhere(boolean isTrue) {
        super(isTrue?new And[]{And.EMPTY()}:new And[0]);
    }

    public ImMap<K,AndClassSet>[] getAnds() {
        return wheres;
    }

    protected And<K>[] newArray(int size) {
        return new And[size];
    }

    protected boolean containsAll(And<K> who, And<K> what) {
        return who.containsAll(what);
    }

    protected And<K> intersect(And<K> where1, And<K> where2) {
        return where1.intersect(where2);
    }

    protected AbstractClassWhere(And<K>[] wheres) {
        super(wheres);
    }

    protected AbstractClassWhere(K key, AndClassSet classes) {
        super(new And<K>(key,classes));
    }

    protected AbstractClassWhere(ImMap<K,? extends AndClassSet> map) {
        super(new And<K>((ImMap<K, AndClassSet>) map));
    }

    public boolean isTrue() {
        return wheres.length==1 && wheres[0].isEmpty();
    }

    public AndClassSet getSingleWhere(K key) {
        AndClassSet result = wheres[0].get(key);
        for(int i=1;i<wheres.length;i++)
            assert result.equals(wheres[i].get(key));
        return result;
    }

    protected abstract This FALSETHIS();

    public This andNot(This where) {
        if(where.isFalse())
            return (This) this;
        This changedWhere = FALSETHIS();
        if(where.isTrue())
            return changedWhere;
        
        And<K>[] rawKeepWheres = newArray(wheres.length); int k=0;
        for(And<K> and : wheres) {
            And<K>[] andNots = and.andNot(where);
            if(andNots!=null) {
                for(And<K> andNot : andNots)
                    changedWhere = changedWhere.or(createThis(new And[]{andNot}));
            } else
                rawKeepWheres[k++] = and;
        }
        if(k==wheres.length)
            return (This) this;
        if(k==0)
            return changedWhere;
        And<K>[] keepWheres = newArray(k); System.arraycopy(rawKeepWheres,0,keepWheres,0,k);
        return changedWhere.or(createThis(keepWheres));
    }

    public boolean means(This where) {
        // берем все перестановки из means, and'им их и проверяем на means всех элементов, сделаем в лоб потому как Combinations слишком громоздкий
        if(where.isFalse()) return isFalse();
        if(where.isTrue() || isFalse()) return true;
        if(isTrue()) return false;

        for(And<K> andWhere : wheres)
            if(!where.meansFrom(andWhere)) return false;
        return true;
    }

    private boolean meansFrom(And<K> andFrom) {
        if(knf==null) {
            Object[][] mwheres = new Object[wheres.length][]; AndClassSet[][] msets=new AndClassSet[wheres.length][]; int[] mnums=new int[wheres.length]; int mnum=0;
            // берем все перестановки из means, and'им их и проверяем на means всех элементов, сделаем в лоб потому как Combinations слишком громоздкий
            for(And<K> where : wheres) { // бежим по всем операндам
                int size = where.size();
                Object[] keys = new Object[size]; AndClassSet[] sets = new AndClassSet[size]; int num=0;
                for(int i=0;i<size;i++) { // бежим по всем элементам
                    K key = where.getKey(i); AndClassSet set = where.getValue(i); AndClassSet fromSet;
                    if((fromSet=andFrom.getPartial(key))==null || fromSet.and(set).isEmpty()) { // если в from'е нету или не пересекаются не интересует
                        num = -1;
                        break;
                    } else
                        if(!set.containsAll(fromSet)) { // если не следует
                            keys[num]=key; sets[num++]=set;
                        }
                }
                if(num==0)
                    return true;
                if(num>0) {
                    mwheres[mnum] = keys;
                    msets[mnum] = sets;
                    mnums[mnum++] = num;
                }
            }

            if(mnum==0)
                return false;
        }

        boolean result = getKNF().meansFrom(andFrom);
//        assert result == getPrevKNF().meansFrom(andFrom);
        return result;

/*        // погнали перестановки
        int[] nums = new int[mnum];
        while(true) {
//            ThisAnd next = createThisAnd();
            Or<K> or = new Or<K>();
            for(int j=0;j<mnum;j++)
                or.add((K)mwheres[j][nums[j]],msets[j][nums[j]].getOr());
            // здесь не так здесь надо проверять каждый на хоть один из этого
            if(!or.meansFrom(andFrom))
                return false;

            int i = 0; // переходим к следующей комбинации
            while(i<mnum && nums[i]==mnums[i]-1) {
                nums[i] = 0;
                i++;
            }
            if(i==mnum)
                return true;
            else
                nums[i]++;
        }*/
    }

    private KNF<K> knf;
    @ManualLazy
    private KNF<K> getKNF() {
        if(knf==null) {
            if(wheres.length==0)
                knf = KNF.STATIC(false);
            else {
                KNF<K>[] knfs = new KNF[wheres.length];
                for (int i = 0; i < wheres.length; i++)
                    knfs[i] = new KNF<K>(KNF.toOr(wheres[i]));

                for(int p=0;p<wheres.length-1;p++) {
                    // сначала ищем наиболее похожие
                    BaseUtils.Paired<Or<K>> max = null; int lm=0; int rm = 0;
                    for(int i=0;i<knfs.length;i++)
                        if(knfs[i] != null) {
                            for(int j=i+1;j<knfs.length;j++)
                                if(knfs[j] != null) {
                                    BaseUtils.Paired<Or<K>> paired = new BaseUtils.Paired<Or<K>>(knfs[i].wheres, knfs[j].wheres, KNF.<K>instancer());
                                    if(max == null || paired.common.length > max.common.length) {
                                        max = paired; lm = i; rm = j;
                                    }
                                }
                        }
                    knfs[lm] = KNF.orPairs(max);
                    knfs[rm] = null;
                }
                knf = knfs[0];
            }
        }
        return knf;
    }
    
    private KNF<K> prevKnf;
    private KNF<K> getPrevKNF() {
        if(prevKnf == null) {
            prevKnf = KNF.STATIC(false);
            for(And<K> where : wheres) // бежим по всем операндам
                prevKnf = prevKnf.or(where);
        }
        return prevKnf;
    }

    public static class Or<K> extends NF<K,OrClassSet> {

        private final static Or empty = new Or(MapFact.EMPTY());
        public static <K> Or<K> EMPTY() {
            return empty;
        }

        public Or(ImMap<K, OrClassSet> map) {
            super(map);
        }

        protected boolean containsAll(OrClassSet who, OrClassSet what) {
            return who.containsAll(what);
        }

        protected Or<K> intersect(Or<K> where2) {
            if(size()>where2.size()) return where2.intersect(this);
            return new Or<K>(map.merge(where2.map, OrObjectClassSet.<K>addOr())); // так быстрее этот участок кода выполняется, ОЧЕНЬ много раз
        }

        public boolean meansFrom(And<K> where) {
            for(int i=0,size=size();i<size;i++) { // так как элементы не зависимы проверим каждый в отдельности
                AndClassSet inSet = where.getPartial(getKey(i));
                if(inSet!=null && getValue(i).containsAll(inSet.getOr())) return true;
            }
            return false;
        }

        public Or(K key, OrClassSet value) {
            super(key, value);
        }
    }

    protected static class KNF<K> extends ExtraMapSetWhere<K,OrClassSet,Or<K>,KNF<K>> {

        private final static ArrayInstancer<Or> instancer = new ArrayInstancer<Or>() {
            public Or[] newArray(int size) {
                return new Or[size];
            }
        };
        public static <K> ArrayInstancer<Or<K>> instancer() {
            return BaseUtils.immutableCast(instancer);
        }

        @Override
        protected Or<K>[] newArray(int size) {
            return instancer.newArray(size);
        }

        public KNF(Or<K>[] iWheres) {
            super(iWheres);
        }

        private final static KNF TRUE = new KNF(new Or[0]);
        private final static KNF FALSE = new KNF(new Or[]{Or.EMPTY()});
        public static <K> KNF<K> STATIC(boolean isTrue) {
            return isTrue ? TRUE : FALSE;
        }
        

        protected KNF<K> createThis(Or<K>[] wheres) {
            return new KNF<K>(wheres);
        }

        // теоретически таже логика что и для and'ов потому как все элементы независимы и соответственно все равно, что and, что or
        public boolean containsAll(Or<K> who, Or<K> what) {
            return who.containsAll(what);
        }

        public Or<K> intersect(Or<K> where1, Or<K> where2) {
            return where1.intersect(where2);
        }

        protected Or<K> createMap(ImMap<K, OrClassSet> map) {
            return new Or<K>(map);
        }

        protected OrClassSet addMapValue(OrClassSet value1, OrClassSet value2) {
            return value1.and(value2);
        }

        public KNF<K> or(ImMap<K,AndClassSet> map) {
            return orPairs(new BaseUtils.Paired<Or<K>>(wheres, toOr(map), KNF.<K>instancer()));
        }

        private static <K> Or<K>[] toOr(ImMap<K, AndClassSet> map) {
            int size = map.size();
            Or<K>[] toOr = new Or[size];
            for(int j=0;j<size;j++)
                toOr[j] = new Or<K>(map.getKey(j),map.getValue(j).getOr());
            return toOr;
        }

        private static <K> KNF<K> orPairs(BaseUtils.Paired<Or<K>> paired) {
            // алгоритм такой : надо до выполнения самого or'а, выцепим "скобки" (единичные or'ы) тогда можно assert'ить что скобки можно просто прицепить и ни одна из этих скобок не будет следовать
            KNF<K> and = new KNF<K>(paired.getDiff1()).intersect(new KNF<K>(paired.getDiff2()));
            if(paired.common.length==0)
                return and;
            assert and.checkMerge(paired.common);
            Or<K>[] merged = new Or[and.wheres.length + paired.common.length];
            System.arraycopy(paired.common,0,merged,0,paired.common.length);
            System.arraycopy(and.wheres,0,merged,paired.common.length,and.wheres.length);
            return new KNF<K>(merged);
        }

        // проверяет что можно тупо слить
        boolean checkMerge(Or<K>[] merge) {
            for(Or<K> mergeOr : merge)
                for(Or<K> where : wheres)
                    assert !where.containsAll(mergeOr) && !mergeOr.containsAll(where);
            return true;
        }

        public boolean meansFrom(And<K> andFrom) {
            for(Or<K> where : wheres)
                if(!where.meansFrom(andFrom)) return false;
            return true;
        }
    }

    private static <K,V,VThis extends AbstractClassWhere<V,VThis>> And<K>[] initMapKeys(VThis classes,ImRevMap<V,K> map) {
        And<K>[] mapWheres = new And[classes.wheres.length];
        for(int i=0;i<classes.wheres.length;i++)
            mapWheres[i] = classes.wheres[i].innerRemap(map);
        return mapWheres;
    }
    protected <V, VThis extends AbstractClassWhere<V,VThis>> AbstractClassWhere(VThis classes,ImRevMap<V,K> map) {
        super(initMapKeys(classes, map));
    }

    protected ImSet<K> keySet() {
        MSet<K> mKeys = SetFact.mSet();
        for(And<K> where : wheres)
            mKeys.addAll(where.keys());
        return mKeys.immutable();
    }

    protected AbstractClassWhere(And<K> where) {
        super(where);
    }

    public <T extends K> ImMap<T, AndClassSet> getCommonClasses(ImSet<T> keys) {
        MMap<T,AndClassSet> mResult = MapFact.mMap(AbstractClassWhere.<T>addOr());
        for(And<K> where : wheres)
            mResult.addAll(where.filterKeys(keys));
        return mResult.immutable();
    }

    public <T extends K> ImMap<T, ValueClass> getCommonParent(ImSet<T> keys) {
        return getCommonClasses(keys).mapValues(new GetValue<ValueClass, AndClassSet>() {
            public ValueClass getMapValue(AndClassSet value) {
                return value.getOr().getCommonClass();
            }
        });
    }

    public AndClassSet getCommonClass(K key) {
        return getCommonClasses(SetFact.singleton(key)).get(key);
    }

    public OrObjectClassSet getOrSet(K key) {
        return (OrObjectClassSet) getCommonClass(key).getOr();
    }

    private static <K> ImMap<K,AndClassSet> initUpClassSets(ImMap<K, ValueClass> map) {
        return map.mapValues(new GetValue<AndClassSet, ValueClass>() {
            public AndClassSet getMapValue(ValueClass value) {
                return value.getUpSet();
            }});
    }
    public AbstractClassWhere(ImMap<K, ValueClass> mapClasses,boolean up) {
        this(initUpClassSets(mapClasses));
        assert up;
    }
}
