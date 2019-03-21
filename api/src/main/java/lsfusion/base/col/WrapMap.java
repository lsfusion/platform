package lsfusion.base.col;

import lsfusion.base.col.implementations.abs.AMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

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
    public <M> ImFilterRevValueMap<K, M> mapFilterRevValues() {
        return map.mapFilterRevValues();
    }


    public <M> ImValueMap<K, M> mapItValues() {
        return map.mapItValues();
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return map.mapItRevValues();
    }
}
