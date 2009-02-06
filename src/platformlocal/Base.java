/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import sun.reflect.ReflectionFactory;

import javax.swing.tree.DefaultMutableTreeNode;
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

    static <T> void RecFillSubSetList(List<T> BuildSet,int Current,List<List<T>> Result,ArrayList<T> CurrentSet) {
        if(Current>=BuildSet.size()) {
            Result.add((List<T>)CurrentSet.clone());
            return;
        }
        
        RecFillSubSetList(BuildSet,Current+1,Result,CurrentSet);
        CurrentSet.add(BuildSet.get(Current));
        RecFillSubSetList(BuildSet,Current+1,Result,CurrentSet);
        CurrentSet.remove(BuildSet.get(Current));
    }
    
    // строит список подмн-в в лексикографическом порядке
    static <T> List<List<T>> buildSubSetList(Collection<T> BuildSet) {

        List<T> BuildList;
        if(BuildSet instanceof List)
            BuildList = (List<T>)BuildSet;
        else
            BuildList = new ArrayList<T>(BuildSet);

        List<List<T>> Result = new ArrayList();
        RecFillSubSetList(BuildList,0,Result,new ArrayList());
        return Result;
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

        Collection<List<T>> result = new ArrayList();
        recBuildSetCombinations(count, listElements, 0, new ArrayList(), result);
        return result;
    }
}

class MapBuilder {
    
    private static<T,V> void RecBuildMap(T[] From,V[] To,int iFr,List<Map<T,V>> Result,HashMap<T,V> CurrentMap) {
        if(iFr==From.length) {
            Result.add((Map<T,V>)CurrentMap.clone());
            return;
        }

        for(int v=0;v<To.length;v++)
            if(!CurrentMap.containsValue(To[v])){
                CurrentMap.put(From[iFr],To[v]);
                RecBuildMap(From,To,iFr+1,Result,CurrentMap);
                CurrentMap.remove(From[iFr]);
            }
    }
    
    static <T,V> List<Map<T,V>> BuildMap(T[] From,V[] To) {
        List<Map<T,V>> Result = new ArrayList<Map<T,V>>();
        RecBuildMap(From,To,0,Result,new HashMap<T,V>(0));
        return Result;
    }

    private static <T,V> void recBuildCombination(Map<T,Collection<V>> Map,ListIterator<T> iT,Collection<Map<T,V>> Result,HashMap<T,V> CurrentMap) {
        if(!iT.hasNext()) {
            Result.add((Map<T,V>)CurrentMap.clone());
            return;
        }

        T Current = iT.next();

        for(V toV : Map.get(Current)) {
            CurrentMap.put(Current,toV);
            recBuildCombination(Map,iT,Result,CurrentMap);
        }

        iT.previous();
    }

    static <T,V> Collection<Map<T,V>> buildCombination(Map<T,Collection<V>> Map) {
        Collection<Map<T,V>> Result = new ArrayList<Map<T,V>>();
        recBuildCombination(Map,(new ArrayList(Map.keySet())).listIterator(),Result,new HashMap<T,V>());
        return Result;
    }

    private static <T,V> void recBuildPairs(Collection<? extends V> Set,ListIterator<T> iT,Collection<Map<T,V>> Result,HashMap<T,V> CurrentMap) {
        if(!iT.hasNext()) {
            Result.add((Map<T,V>)CurrentMap.clone());
            return;
        }

        T Current = iT.next();

        for(V toV : Set)
            if(!CurrentMap.values().contains(toV)){
                CurrentMap.put(Current,toV);
                recBuildPairs(Set,iT,Result,CurrentMap);
                CurrentMap.remove(Current);
            }

        iT.previous();
    }

    static <T,V> Collection<Map<T,V>> buildPairs(Collection<? extends T> Set1,Collection<? extends V> Set2) {
        if(Set1.size()!=Set2.size()) return null;
        Collection<Map<T,V>> Result = new ArrayList<Map<T,V>>();
        recBuildPairs(Set2,(new ArrayList(Set1)).listIterator(),Result,new HashMap<T,V>());
        return Result;
    }

    private static <T> void recBuildPermutations(Collection<T> col, List<T> cur, Collection<List<T>> result) {

        if (cur.size() == col.size()) { result.add(new ArrayList(cur)); return; }

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
    static <K,V> void removeAll(Map<K,V> Map,Collection<? extends K> Keys) {
        for(K Key : Keys)
            Map.remove(Key);
    }

    static void removeAll(Collection<?> From,Collection<?> Remove) {
        for(Object Key : Remove)
            From.remove(Key);
    }

    static <V> V getRandom(Set<V> ToRandom,Random Randomizer) {
        int RandomNum = Randomizer.nextInt(ToRandom.size());
        int CountNum = 0;
        for(V Object : ToRandom)
            if(CountNum==RandomNum) return Object;
        return null;
    }

    static <T> Set<T> intersect(Set<T> Op1,Set<T> Op2) {
        Set<T> Result = new HashSet<T>();
        for(T Element : Op1)
            if(Op2.contains(Element)) Result.add(Element);
        return Result;
    }

    static <K,MK> Map<K,MK> reverse(Map<MK,K> Map) {
        Map<K,MK> ReverseKeys = new HashMap<K,MK>();
        for(Map.Entry<MK,K> MapKey : Map.entrySet())
            ReverseKeys.put(MapKey.getValue(),MapKey.getKey());
        return ReverseKeys;
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
}