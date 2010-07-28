package platform.base;

import java.util.HashMap;
import java.util.Map;

public class ReversedHashMap<K,V> extends HashMap<K, V> implements ReversedMap<K,V> {

    public ReversedHashMap() {
    }

    public ReversedHashMap(Map<? extends K, ? extends V> map) {
        super(map);
    }

    public ReversedMap<V, K> reverse() {
        ReversedHashMap<V, K> result = new ReversedHashMap<V, K>();
        for(Map.Entry<K,V> entry : entrySet())
            result.put(entry.getValue(),entry.getKey());
        return result;
    }
}
