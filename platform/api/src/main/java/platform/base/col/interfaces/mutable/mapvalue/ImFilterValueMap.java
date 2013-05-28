package platform.base.col.interfaces.mutable.mapvalue;

import platform.base.col.interfaces.immutable.ImMap;

public interface ImFilterValueMap<K, V> {

    void mapValue(int i, V value);

    ImMap<K, V> immutableValue();
}
