package platform.base.col.interfaces.mutable;

import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;

public interface MOrderSet<K> {

    void add(K key);
    void addAll(ImOrderSet<? extends K> set);

    ImOrderSet<K> immutableOrder();

}
