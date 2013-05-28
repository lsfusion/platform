package platform.base.col.interfaces.mutable;

import platform.base.col.interfaces.immutable.ImCol;

public interface MCol<K> {

    void add(K key);
    int size();

    void addAll(ImCol<? extends K> col);
    
    ImCol<K> immutableCol();
}
