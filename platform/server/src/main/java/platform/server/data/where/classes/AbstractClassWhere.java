package platform.server.data.where.classes;

import platform.base.ArrayInstancer;
import platform.base.BaseUtils;
import platform.base.QuickMap;
import platform.base.SetWhere;
import platform.server.caches.ManualLazy;
import platform.server.logics.BusinessLogics;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.sets.OrClassSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


// !!! equals'ы и hashCode должны только в meanWheres вызываться
// предполагается ДНФ потому как в отличии от КНФ у Set'ов пустое пересечение актуально в данной задаче в то время как универсального мн-ва здесь нету 
public abstract class AbstractClassWhere<K, This extends AbstractClassWhere<K, This>> extends SetWhere<AbstractClassWhere.And<K>,This> {

    public This or(This where) {
        return add(where);
    }
    public This and(This where) {
        return intersect(where);
    }

    protected static class And<K> extends QuickMap<K, AndClassSet> {

        @Override
        public AndClassSet get(K key) {
            AndClassSet result = super.get(key);
            if(result==null && !BusinessLogics.checkClasses)
                throw new RuntimeException();
            assert BusinessLogics.checkClasses || result!=null;
            return result;
        }

        public AndClassSet getPartial(K key) {
            return super.get(key);
        }

        public boolean compatible(And<K> and) {
            for(int i=0;i<size;i++)
                if(!getValue(i).getType().isCompatible(and.get(getKey(i)).getType()))
                    return false;
            return true;
        }

        protected AndClassSet addValue(AndClassSet prevValue, AndClassSet newValue) {
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

        public And(QuickMap<? extends K, AndClassSet> set) {
            super((QuickMap<K,AndClassSet>) set);
        }

        public And(K key, AndClassSet value) {
            super(key, value);
        }

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

    protected And<K>[] intersect(And<K> where1, And<K> where2) {
        if(where1.size>where2.size) return intersect(where2,where1); // пусть добавляется в большую

        And<K> result = new And<K>(where2);
        if(!result.addAll(where1))
            return newArray(0);
        return toArray(result);
    }

    protected AbstractClassWhere(And<K>[] iWheres) {
        super(iWheres);
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
        assert (wheres.length == 1);
        return wheres[0].get(key);
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

        return getKNF().meansFrom(andFrom);

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
            knf = KNF.STATIC(false);
            for(And<K> where : wheres) // бежим по всем операндам
                knf = knf.or(where);
        }
        return knf;
    }

    static class Or<K> extends QuickMap<K,OrClassSet> {

        Or() { }
        Or(Or<K> set) { super(set); }

        protected OrClassSet addValue(OrClassSet prevValue, OrClassSet newValue) {
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

    protected static class KNF<K> extends SetWhere<Or<K>,KNF<K>> implements ArrayInstancer<Or<K>> {

        public Or<K>[] newArray(int size) {
            return new Or[size];
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

        protected KNF<K> createThis(Or<K>[] iWheres) {
            return new KNF<K>(iWheres);
        }

        // теоретически таже логика что и для and'ов потому как все элементы независимы и соответственно все равно, что and, что or
        public boolean containsAll(Or<K> who, Or<K> what) {
            return what.containsAll(who);
        }

        public Or<K>[] intersect(Or<K> where1, Or<K> where2) {
            if(where1.size>where2.size) return intersect(where2,where1); // пусть добавляется в большую

            Or<K> result = new Or<K>(where2);
            result.addAll(where1);
            return toArray(result);
        }

        public KNF<K> or(QuickMap<K,AndClassSet> map) {
            // алгоритм такой : надо до выполнения самого or'а, выцепим "скобки" (единичные or'ы) тогда можно assert'ить что скобки можно просто прицепить и ни одна из этих скобок не будет следовать

            Or<K>[] toOr = newArray(map.size);
            for(int j=0;j<map.size;j++)
                toOr[j] = new Or<K>(map.getKey(j),map.getValue(j).getOr());

            BaseUtils.Paired<Or<K>> paired = new BaseUtils.Paired<Or<K>>(wheres, toOr, this);
            KNF<K> and = new KNF<K>(paired.getDiff1()).intersect(new KNF<K>(paired.getDiff2()));
            if(paired.common.length==0)
                return and;
            assert and.checkMerge(paired.common);
            Or<K>[] merged = newArray(and.wheres.length+paired.common.length);
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
        for(int i=0;i<classes.wheres.length;i++) {
            mapWheres[i] = new And<K>();
            for(int j=0;j<classes.wheres[i].size;j++)
                mapWheres[i].add(map.get(classes.wheres[i].getKey(j)),classes.wheres[i].getValue(j));
        }
        return mapWheres;
    }
    protected <V, VThis extends AbstractClassWhere<V,VThis>> AbstractClassWhere(VThis classes,Map<V,K> map) {
        super(initMapKeys(classes, map));
    }

    protected Collection<K> keySet() {
        Set<K> keys = new HashSet<K>();
        for(And<K> where : wheres)
            for(int i=0;i<where.size;i++)
                keys.add(where.getKey(i));
        return keys;
    }

    protected AbstractClassWhere(And<K> where) {
        super(where);
    }
}
