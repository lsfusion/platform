package lsfusion.base.col.interfaces.mutable.mapvalue;

import lsfusion.base.col.interfaces.immutable.ImRevMap;

public interface ImRevValueMap<K, V> {
    void mapValue(int i, V value);

    V getMapValue(int i);
    K getMapKey(int i);
    int mapSize();

    ImRevMap<K, V> immutableValueRev();
}
