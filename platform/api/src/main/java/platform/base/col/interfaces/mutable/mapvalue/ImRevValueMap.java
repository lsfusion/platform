package platform.base.col.interfaces.mutable.mapvalue;

import platform.base.col.interfaces.immutable.ImRevMap;

public interface ImRevValueMap<K, V> {
    void mapValue(int i, V value);

    ImRevMap<K, V> immutableValueRev();
}
