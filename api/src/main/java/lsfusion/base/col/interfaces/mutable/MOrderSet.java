package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;

public interface MOrderSet<K> {

    void add(K key);
    void addAll(ImOrderSet<? extends K> set);

    ImOrderSet<K> immutableOrder();

}
