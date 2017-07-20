package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;

public interface MOrderSet<K> {

    boolean add(K key);
    void addAll(ImOrderSet<? extends K> set);

    ImOrderSet<K> immutableOrder();

}
