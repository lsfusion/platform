/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import sun.reflect.ReflectionFactory;

import java.util.*;
import java.lang.*;
import java.lang.Class;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author ME2
 */

class SetBuilder<T> {

    static <T> void recFillSubSetList(List<T> buildSet,int current,List<List<T>> result,ArrayList<T> currentSet) {
        if(current>=buildSet.size()) {
            result.add((List<T>)currentSet.clone());
            return;
        }
        
        recFillSubSetList(buildSet,current+1,result,currentSet);
        currentSet.add(buildSet.get(current));
        recFillSubSetList(buildSet,current+1,result,currentSet);
        currentSet.remove(buildSet.get(current));
    }
    
    // строит список подмн-в в лексикографическом порядке
    static <T> List<List<T>> buildSubSetList(Collection<T> buildSet) {

        List<T> buildList;
        if(buildSet instanceof List)
            buildList = (List<T>)buildSet;
        else
            buildList = new ArrayList<T>(buildSet);

        List<List<T>> result = new ArrayList<List<T>>();
        recFillSubSetList(buildList,0,result,new ArrayList<T>());
        return result;
    }

    public static <T> void recBuildSetCombinations(int count, List<T> listElements, int current, ArrayList<T> currentList, Collection<List<T>> result) {

        if (currentList.size() == count) {
            result.add((List<T>)currentList.clone());
            return;
        }

        if (current >= listElements.size()) return;

        recBuildSetCombinations(count, listElements, current+1, currentList, result);
        currentList.add(listElements.get(current));
        recBuildSetCombinations(count, listElements, current+1, currentList, result);
        currentList.remove(listElements.get(current));
    }

    public static <T> Collection<List<T>> buildSetCombinations(int count, List<T> listElements) {

        Collection<List<T>> result = new ArrayList<List<T>>();
        recBuildSetCombinations(count, listElements, 0, new ArrayList<T>(), result);
        return result;
    }
}

class MapBuilder {

    private static <T> void recBuildPermutations(Collection<T> col, List<T> cur, Collection<List<T>> result) {

        if (cur.size() == col.size()) { result.add(new ArrayList<T>(cur)); return; }

        for (T element : col) {
            if (!cur.contains(element)) {
                cur.add(element);
                recBuildPermutations(col, cur, result);
                cur.remove(element);
            }
        }
    }

    public static <T> Collection<List<T>> buildPermutations(Collection<T> col) {

        Collection<List<T>> result = new ArrayList<List<T>>();
        recBuildPermutations(col, new ArrayList<T>(), result);
        return result;
    }
}

class Combinations<T,V> implements Iterable<Map<T,V>> {
    
    public Iterator<Map<T, V>> iterator() {
        return new CombinationIterator();
    }

    List<T> from = new ArrayList<T>();
    List<List<V>> to = new ArrayList<List<V>>();
    Combinations(Map<T,Collection<V>> map) {
        for(Map.Entry<T,Collection<V>> entry : map.entrySet()) {
            from.add(entry.getKey());
            to.add(new ArrayList<V>(entry.getValue()));
        }
    }

    class CombinationIterator implements Iterator<Map<T,V>> {

        int[] nums;
        int size;

        CombinationIterator() {
            for(List<V> list : to)
                if(list.size()==0) {
                    hasNext = false;
                    return;
                }
            size = from.size();
            nums = new int[size];
        }

        boolean hasNext = true;
        public boolean hasNext() {
            return hasNext;
        }

        public Map<T, V> next() {

            Map<T,V> next = new HashMap<T,V>();
            for(int i=0;i<size;i++)
                next.put(from.get(i),to.get(i).get(nums[i]));

            // переходим к следующей паре
            int i = 0;
            while(i<size && nums[i]==to.get(i).size()-1) {
                nums[i] = 0;
                i++;
            }
            if(i==size)
                hasNext = false;
            else
                nums[i]++;

            return next;
        }

        public void remove() { // не поддерживает
        }
    }
}

abstract class Permutations<Permute> implements Iterable<Permute> {

    int size;

    public Iterator<Permute> iterator() {
        return new PermuteIterator();
    }

    abstract Permute getPermute(PermuteIterator permute);

    class PermuteIterator implements Iterator<Permute> {

        int[] nums;

        PermuteIterator() {
            if(size<0)
                hasNext = false;
            else {
                nums = new int[size];
                for(int i=0;i<size;i++) // начальная перестановка
                    nums[i] = i;
            }
        }

        boolean hasNext = true;
        public boolean hasNext() {
            return hasNext;
        }
        
        public Permute next() {

            Permute next = getPermute(this);

            int i=size-1;
            while(i>=1 && nums[i-1]>nums[i]) i--; // находим первый нарушающий порядок
            if(i<=0)
                hasNext = false;
            else {
                // находим минимальный элемент больше это
                int min = i;
                for(int j=i+1;j<size;j++)
                    if(nums[j]>nums[i-1] && nums[j]<nums[min])
                        min = j;
                int t = nums[i-1]; nums[i-1] = nums[min]; nums[min] = t;
                for(int j=0;j<(size-i)/2;j++) { // переворачиваем
                    t = nums[i+j]; nums[i+j] = nums[size-1-j]; nums[size-1-j] = t; }
            }

            return next;
        }

        public void remove() { // не поддерживает
        }
    }
}

class Pairs<T,V> extends Permutations<Map<T,V>> {

    List<T> from;
    List<V> to;
    Pairs(Collection<? extends T> iFrom, Collection<? extends V> iTo) {
        from = new ArrayList<T>(iFrom);
        to = new ArrayList<V>(iTo);
        if((size=from.size())!=to.size())
            size = -1;
    }

    Map<T, V> getPermute(PermuteIterator permute) {
        Map<T,V> next = new HashMap<T,V>();
        for(int i=0;i<size;i++)
            next.put(from.get(i),to.get(permute.nums[i]));
        return next;
    }

    boolean isEmpty() {
        return from.size()!=to.size();
    }
}

class ListPermutations<T> extends Permutations<List<T>> {

    List<T> to;

    ListPermutations(Collection<T> toCollection) {
        to = new ArrayList<T>(toCollection);
        size = to.size();
    }

    List<T> getPermute(PermuteIterator permute) {
        List<T> result = new ArrayList<T>();
        for(int i=0;i<size;i++)
            result.add(to.get(permute.nums[i]));
        return result;
    }
}

class MapUtils<T,V> {
    
    public T getKey(Map<T,V> m, V v) {

        Iterator<T> it = m.keySet().iterator();
        while (it.hasNext()) {
           T t = it.next();
           if (m.get(t) == v) return t;
        }
        return null;
        
    }
    
}

class CollectionExtend {
    static <K,V> void removeAll(Map<K,V> map,Collection<? extends K> ks) {
        for(K Key : ks)
            map.remove(Key);
    }

    static <V> V getRandom(Set<V> toRandom,Random randomizer) {
        int randomNum = randomizer.nextInt(toRandom.size());
        int countNum = 0;
        for(V object : toRandom)
            if(countNum==randomNum) return object;
        return null;
    }

    static <T> Set<T> intersect(Set<T> op1,Set<T> op2) {
        Set<T> result = new HashSet<T>();
        for(T element : op1)
            if(op2.contains(element)) result.add(element);
        return result;
    }

    static <K,MK> Map<K,MK> reverse(Map<MK,K> map) {
        Map<K,MK> reverseKeys = new HashMap<K,MK>();
        for(Map.Entry<MK,K> mapKey : map.entrySet())
            reverseKeys.put(mapKey.getValue(),mapKey.getKey());
        return reverseKeys;
    }

    static <K> Set<K> add(Set<K> set1,Set<K> set2) {
        HashSet<K> result = new HashSet<K>(set1);
        result.addAll(set2);
        return result;
    }
}

class Pair<Class1, Class2> {

    Class1 first;
    Class2 second;

    public Pair(Class1 ifirst, Class2 isecond) {
        first = ifirst;
        second = isecond;
    }

    public String toString() { return first.toString(); }

}

class DateConverter {

    public static Integer dateToInt(Date date) {

        if (date == null) return null;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return ((calendar.get(Calendar.YEAR) < 2000) ? -1 : 1) *(Math.abs(calendar.get(Calendar.YEAR) - 2000) * 10000 + calendar.get(Calendar.MONTH) * 100 + calendar.get(Calendar.DATE));
    }

    public static Date intToDate(Integer num) {

        if (num == null) return null;
        
        Calendar calendar = Calendar.getInstance();
        if (num < 0)
            calendar.set(2000 - (-num) / 10000, (-num / 100) % 100, -num % 100);
        else
            calendar.set(num / 10000 + 2000, (num / 100) % 100, num % 100);
        return calendar.getTime();
    }


}

class BaseUtils {

    public static boolean compareObjects(Object obj1, Object obj2) {

        if (obj1 == null)
            return obj2 == null;
        else
            return obj1.equals(obj2);
    }

    public static <T> boolean findByReference(Collection<T> col, Object obj) {
        for (T objCol : col)
            if (objCol == obj) return true;
        return false;
    }

    private static ReflectionFactory reflectionFactory = ReflectionFactory.getReflectionFactory();

    public static Object getDefaultValue(java.lang.Class cls) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {

        Constructor javaLangObjectConstructor = Object.class.getDeclaredConstructor(new Class[0]);
        Constructor customConstructor = reflectionFactory.newConstructorForSerialization(cls, javaLangObjectConstructor);
        return customConstructor.newInstance(new Object[0]);
    }

    public static Object multiply(Object obj, Integer coeff) {

        if (obj instanceof Integer) return ((Integer)obj) * coeff;
        if (obj instanceof Long) return ((Long)obj) * coeff;
        if (obj instanceof Double) return ((Double)obj) * coeff;

        return obj;
    }

    static <KA,VA,KB,VB> boolean mapEquals(Map<KA,VA> mapA,Map<KB,VB> mapB,Map<KA,KB> mapAB) {
        for(Map.Entry<KA,VA> A : mapA.entrySet())
            if(!mapB.get(mapAB.get(A.getKey())).equals(A.getValue()))
                return false;    
        return true;
    }

    static <K,E,V> Map<K,V> join(Map<K,E> map, Map<E,V> joinMap) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,E> entry : map.entrySet())
            result.put(entry.getKey(),joinMap.get(entry.getValue()));
        return result;
    }

    static <K,E,V> LinkedHashMap<V,E> linkedJoin(LinkedHashMap<K,E> map, Map<K,V> joinMap) {
        LinkedHashMap<V,E> result = new LinkedHashMap<V, E>();
        for(Map.Entry<K,E> entry : map.entrySet())
            result.put(joinMap.get(entry.getKey()),entry.getValue());
        return result;
    }

    static <K,E,V> Map<K,V> innerJoin(Map<K,E> map, Map<E,V> joinMap) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,E> entry : map.entrySet()) {
            V joinValue = joinMap.get(entry.getValue());
            if(joinValue!=null) result.put(entry.getKey(), joinValue);
        }
        return result;
    }

    static <K,V> Map<K,V> filter(Map<K,V> map, Collection<V> values) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,V> entry : map.entrySet())
            if(values.contains(entry.getValue()))
                result.put(entry.getKey(),entry.getValue());
        return result;        
    }

    static <K,V> Map<V,K> reverse(Map<K,V> map) {
        Map<V,K> result = new HashMap<V, K>();
        for(Map.Entry<K,V> entry : map.entrySet())
            result.put(entry.getValue(),entry.getKey());
        return result;
    }

    static <KA,VA,KB,VB> Map<VA,VB> crossJoin(Map<KA,VA> map,Map<KB,VB> mapTo,Map<KA,KB> mapJoin) {
        return join(join(reverse(map),mapJoin),mapTo);
    }

    static <K,V> boolean identity(Map<K,V> map) {
        for(Map.Entry<K,V> entry : map.entrySet())
            if(!entry.getKey().equals(entry.getValue())) return false;
        return true;
    }

    static <K> Map<K,K> toMap(Collection<K> collection) {
        Map<K,K> result = new HashMap<K, K>();
        for(K object : collection)
            result.put(object,object);
        return result;
    }
}