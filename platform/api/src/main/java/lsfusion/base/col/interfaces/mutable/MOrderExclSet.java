package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;

public interface MOrderExclSet<K> {

    void exclAdd(K key);
    void exclAddAll(ImOrderSet<? extends K> set);

    ImOrderSet<K> immutableOrder();
}
