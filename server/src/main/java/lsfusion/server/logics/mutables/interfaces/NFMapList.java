package lsfusion.server.logics.mutables.interfaces;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;

public interface NFMapList<K, V> extends NFMapCol<K, V> {
    
    ImMap<K, ImList<V>> getOrderMap();
    
    Iterable<V> getListIt(K key);
}
