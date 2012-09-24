package platform.server.data.where.classes;

import platform.base.*;
import platform.server.caches.ManualLazy;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.OrClassSet;
import platform.server.classes.sets.OrObjectClassSet;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;

import java.util.*;


// !!! equals'ы и hashCode должны только в meanWheres вызываться
// предполагается ДНФ потому как в отличии от КНФ у Set'ов пустое пересечение актуально в данной задаче в то время как универсального мн-ва здесь нету 
public abstract class AbstractClassWhere<K, This extends AbstractClassWhere<K, This>> extends ExtraMapSetWhere<K,AndClassSet,AbstractClassWhere.And<K>,This> {

    public This or(This where) {
        return add(where);
    }
    public This and(This where) {
        return intersect(where);
    }

    public static class And<K> extends QuickMap<K, AndClassSet> {

        @Override
        public boolean add(K key, AndClassSet value) {
            assert value!=null; // не зачем null'ы лучше вообще не добавлять
            return super.add(key, value);
        }

        public boolean compatible(And<K> and) {
            for(int i=0;i<size;i++)
                if(getValue(i).getType().getCompatible(and.get(getKey(i)).getType())==null)
                    return false;
            return true;
        }
        
        public <T> And<T> remap(Map<K, ? extends T> remap) {
            And<T> result = new And<T>();
            for(int i=0;i<size;i++)
                result.add(remap.get(getKey(i)), getValue(i));
            return result;
        }
        
        public And<K> remove(Collection<? extends K> keys) {
            And<K> result = new And<K>();
            for(int i=0;i<size;i++) {
                K key = getKey(i);
                if(!keys.contains(key))
                    result.add(key, getValue(i));
            }
            return result;
        }

        public <T extends K> And<T> keep(Collection<T> keys) {
            And<T> result = new And<T>();
            for(int i=0;i<size;i++) {
                K key = getKey(i);
                if(keys.contains(key))
                    result.add((T) key, getValue(i));
            }
            return result;
        }

        protected AndClassSet addValue(K key, AndClassSet prevValue, AndClassSet newValue) {
            AndClassSet andValue = prevValue.and(newValue);
            if(andValue.isEmpty())
                return null;
            else
                return andValue;
        }

        protected boolean containsAll(AndClassSet who, AndClassSet what) {
            return who.containsAll(what);
        }

        public And() {
        }

        public <V> And(And<V> andWhere, Map<V, K> map) {
            for(int j=0;j< andWhere.size;j++) {
                K mapValue = map.get(andWhere.getKey(j));
                if(mapValue!=null)
                    add(mapValue, andWhere.getValue(j));
            }
        }

        public And(QuickMap<? extends K, AndClassSet> set) {
            super((QuickMap<K,AndClassSet>) set);
        }

        public And(K key, AndClassSet value) {
            super(key, value);
        }

        public Where getWhere(Map<K, ? extends Expr> mapExprs) {
            Where result = Where.TRUE;
            for(int i=0;i<size;i++)
                result = result.and(mapExprs.get(getKey(i)).isClass(getValue(i)));
            return result;
        }

        public And<K>[] andNot(AbstractClassWhere<K, ?> where) {
            AndClassSet[][] ands = new AndClassSet[size][];
            for(int i=0;i<size;i++)
                ands[i] = getValue(i).getAnd();
            ArrayCombinations<AndClassSet> combs = new ArrayCombinations<AndClassSet>(ands, AndClassSet.arrayInstancer);
            if(combs.max==1) {
                if(where.meansFrom(this))
                    return new And[0];
                else
                    return null; // не меняем ничего
            }
            And<K>[] keep = new And[combs.max]; int k=0;
            for(AndClassSet[] comb : combs) {
                And<K> and = new And<K>();
                for(int i=0;i<size;i++)
                    and.add(getKey(i), comb[i]);
                if(!where.meansFrom(and))
                    keep[k++] = and;
            }
            if(combs.max==k)
                return null;
            And<K>[] result = new And[k]; System.arraycopy(keep, 0, result, 0, k);
            return result;
        }
    }

    protected And<K> createMap() {
        return new And<K>();
    }

    protected AndClassSet addMapValue(AndClassSet value1, AndClassSet value2) {
        return value1.or(value2);
    }

    protected AbstractClassWhere(boolean isTrue) {
        super(isTrue?new And[]{new And<K>()}:new And[0]);
    }

    public QuickMap<K,AndClassSet>[] getAnds() {
        return wheres;
    }

    protected And<K>[] newArray(int size) {
        return new And[size];
    }

    protected boolean containsAll(And<K> who, And<K> what) {
        return who.containsAll(what);
    }

    protected And<K> intersect(And<K> where1, And<K> where2) {
        if(where1.size>where2.size) return intersect(where2,where1); // пусть добавляется в большую

        And<K> result = new And<K>(where2);
        if(!result.addAll(where1))
            return null;
        return result;
    }

    protected AbstractClassWhere(And<K>[] wheres) {
        super(wheres);
    }

    protected AbstractClassWhere(K key, AndClassSet classes) {
        super(new And<K>(key,classes));
    }

    private static <K> And<K> initAndClassSets(Map<K,? extends AndClassSet> map) {
        And<K> result = new And<K>();
        for(Map.Entry<K,? extends AndClassSet> entry : map.entrySet())
            result.add(entry.getKey(),entry.getValue());
        return result;
    }
    protected AbstractClassWhere(Map<K,? extends AndClassSet> map) {
        super(initAndClassSets(map));
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

    protected abstract This FALSE();

    public This andNot(This where) {
        if(where.isFalse())
            return (This) this;
        This changedWhere = FALSE();
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
                Object[] keys = new Object[where.size]; AndClassSet[] sets = new AndClassSet[where.size]; int num=0;
                for(int i=0;i<where.size;i++) { // бежим по всем элементам
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

    static class Or<K> extends QuickMap<K,OrClassSet> {

        Or() { }
        Or(Or<K> set) { super(set); }

        protected OrClassSet addValue(K key, OrClassSet prevValue, OrClassSet newValue) {
            return prevValue.or(newValue);
        }

        protected boolean containsAll(OrClassSet who, OrClassSet what) {
            return who.containsAll(what);
        }

        public boolean meansFrom(And<K> where) {
            for(int i=0;i<size;i++) { // так как элементы не зависимы проверим каждый в отдельности
                AndClassSet inSet = where.getPartial(getKey(i));
                if(inSet!=null && getValue(i).containsAll(inSet.getOr())) return true;
            }
            return false;
        }

        Or(K key, OrClassSet value) {
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

        private KNF(boolean isTrue) {
            super(isTrue?new Or[0]:new Or[]{new Or<K>()});
        }
        public static <K> KNF<K> STATIC(boolean isTrue) {
            return new KNF<K>(isTrue);
        }

        protected KNF<K> createThis(Or<K>[] wheres) {
            return new KNF<K>(wheres);
        }

        // теоретически таже логика что и для and'ов потому как все элементы независимы и соответственно все равно, что and, что or
        public boolean containsAll(Or<K> who, Or<K> what) {
            return who.containsAll(what);
        }

        public Or<K> intersect(Or<K> where1, Or<K> where2) {
            if(where1.size>where2.size) return intersect(where2,where1); // пусть добавляется в большую

            Or<K> result = new Or<K>(where2);
            result.addAll(where1);
            return result;
        }

        protected Or<K> createMap() {
            return new Or<K>();
        }

        protected OrClassSet addMapValue(OrClassSet value1, OrClassSet value2) {
            return value1.and(value2);
        }

        public KNF<K> or(QuickMap<K,AndClassSet> map) {
            return orPairs(new BaseUtils.Paired<Or<K>>(wheres, toOr(map), KNF.<K>instancer()));
        }

        private static <K> Or<K>[] toOr(QuickMap<K, AndClassSet> map) {
            Or<K>[] toOr = new Or[map.size];
            for(int j=0;j<map.size;j++)
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

    private static <K,V,VThis extends AbstractClassWhere<V,VThis>> And<K>[] initMapKeys(VThis classes,Map<V,K> map) {
        And<K>[] mapWheres = new And[classes.wheres.length];
        for(int i=0;i<classes.wheres.length;i++)
            mapWheres[i] = new And<K>(classes.wheres[i], map);
        return mapWheres;
    }
    protected <V, VThis extends AbstractClassWhere<V,VThis>> AbstractClassWhere(VThis classes,Map<V,K> map) {
        super(initMapKeys(classes, map));
    }

    protected Set<K> keySet() {
        Set<K> keys = new HashSet<K>();
        for(And<K> where : wheres)
            for(int i=0;i<where.size;i++)
                keys.add(where.getKey(i));
        return keys;
    }

    protected AbstractClassWhere(And<K> where) {
        super(where);
    }

    // с проверкой на null, по аналогии с Action'ом, см. CalcProperty.getCommonClasses
    private static OrClassSet or(OrClassSet or1, OrClassSet or2) {
        if(or1==null)
            return or2;
        if(or2==null)
            return or1;
        return or1.or(or2);
    }
    public <T extends K> Map<T, ValueClass> getCommonParent(Collection<T> keys) {

        Map<T, ValueClass> result = new HashMap<T, ValueClass>();
        for(T key : keys) {
            OrClassSet orSet = null;
            for (And<K> where : wheres) {
                AndClassSet and = where.get(key);
                orSet = or(orSet, and == null ? null : and.getOr());
            }
            if(orSet!=null)
                result.put(key,orSet==null ? null : orSet.getCommonClass());
        }
        return result;
    }

    public OrObjectClassSet getOrSet(K key) {
        OrObjectClassSet orSet = OrObjectClassSet.FALSE;
        for(AbstractClassWhere.And<K> and : wheres) {
            AndClassSet andClass = and.get(key);
            if(andClass!=null)
                orSet = orSet.or(andClass.getOr());
            else
                return null;
        }
        return orSet;
    }

    private static <K> Map<K,AndClassSet> initUpClassSets(Map<K, ValueClass> map) {
        Map<K, AndClassSet> result = new HashMap<K,AndClassSet>();
        for(Map.Entry<K, ValueClass> entry : map.entrySet())
            result.put(entry.getKey(),entry.getValue().getUpSet());
        return result;
    }
    public AbstractClassWhere(Map<K, ValueClass> mapClasses,boolean up) {
        this(initUpClassSets(mapClasses));
        assert up;
    }
}
