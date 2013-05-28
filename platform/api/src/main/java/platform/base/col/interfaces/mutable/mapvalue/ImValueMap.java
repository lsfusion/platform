package platform.base.col.interfaces.mutable.mapvalue;

import platform.base.col.interfaces.immutable.ImMap;

public interface ImValueMap<K,V> {
    void mapValue(int i, V value);

    V getMapValue(int i); // редко, но надо
    K getMapKey(int i); // редко, но надо
    int mapSize();

    ImMap<K, V> immutableValue();
}
