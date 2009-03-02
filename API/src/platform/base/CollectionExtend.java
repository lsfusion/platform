package platform.base;

import java.util.*;

public class CollectionExtend {
    static <K,V> void removeAll(Map<K,V> map, Collection<? extends K> ks) {
        for(K Key : ks)
            map.remove(Key);
    }

    public static <V> V getRandom(Set<V> toRandom, Random randomizer) {
        int randomNum = randomizer.nextInt(toRandom.size());
        int countNum = 0;
        for(V object : toRandom)
            if(countNum==randomNum) return object;
        return null;
    }

    public static <T> Set<T> intersect(Set<T> op1,Set<T> op2) {
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
