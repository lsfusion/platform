package platform.base.col;

import platform.base.col.implementations.abs.AMap;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.mapvalue.*;

public class WrapMap<K, V> extends AMap<K, V> implements ImMap<K, V> {

    protected ImMap<K, V> map;

    public WrapMap(ImMap<? extends K, ? extends V> map) {
        this.map = (ImMap<K, V>) map;
    }

    public WrapMap(K key, V value) {
        this(MapFact.singleton(key, value));
    }

    public int size() {
        return map.size();
    }

    public K getKey(int i) {
        return map.getKey(i);
    }

    public V getValue(int i) {
        return map.getValue(i);
    }

    @Override
    public V getObject(Object key) {
        return map.getObject(key);
    }

    public <M> ImFilterValueMap<K, M> mapFilterValues() {
        return map.mapFilterValues();
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return map.mapItValues();
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return map.mapItRevValues();
    }
}
