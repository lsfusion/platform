package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;

public interface MOrderExclSet<K> {

    void exclAdd(K key);
    void exclAddAll(ImOrderSet<? extends K> set);

    boolean contains(K key);
    int size();
    K get(int i);

    ImOrderSet<K> immutableOrder();
}
