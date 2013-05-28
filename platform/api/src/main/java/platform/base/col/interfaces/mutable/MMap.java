package platform.base.col.interfaces.mutable;

import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;

public interface MMap<K, V> {
    
    boolean add(K key, V value);
    boolean addAll(ImMap<? extends K, ? extends V> map);

    public ImMap<K, V> immutable();

    ImMap<K, V> immutableCopy();
}
