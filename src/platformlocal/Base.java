/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

/**
 *
 * @author ME2
 */

class SetBuilder<T> {

    void RecFillSubSetList(List<T> BuildSet,int Current,List<List<T>> Result,ArrayList<T> CurrentSet) {
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
    List<List<T>> BuildSubSetList(Collection<T> BuildSet) {

        List<T> BuildList;
        if(BuildSet instanceof List)
            BuildList = (List<T>)BuildSet;
        else
            BuildList = new ArrayList<T>(BuildSet);

        List<List<T>> Result = new ArrayList();
        RecFillSubSetList(BuildList,0,Result,new ArrayList());
        return Result;
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
    static void removeAll(Map<?,?> Map,Set<?> Keys) {
        for(Object Key : Keys)
            Map.remove(Key);
    }

    static void removeAll(Collection<?> From,Collection<?> Remove) {
        for(Object Key : Remove)
            From.remove(Key);
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

    public static int dateToInt(Date date) {

//        if (date == null) return 0;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return (calendar.get(Calendar.YEAR) - 2000) * 10000 + calendar.get(Calendar.MONTH) * 100 + calendar.get(Calendar.DATE);
    }

    public static Date intToDate(int num) {

//        if (num == 0) return null;
        
        Calendar calendar = Calendar.getInstance();
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
}