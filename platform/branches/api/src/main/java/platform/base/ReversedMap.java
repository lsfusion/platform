package platform.base;

import java.util.Map;

public interface ReversedMap<K,V> extends Map<K,V> {
    ReversedMap<V,K> reverse();
}
