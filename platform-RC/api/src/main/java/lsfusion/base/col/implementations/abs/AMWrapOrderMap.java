package lsfusion.base.col.implementations.abs;

import lsfusion.base.col.interfaces.immutable.ImMap;

// если аналогичный Map сам по себе ordered
public abstract class AMWrapOrderMap<K, V, W extends AMRevMap<K, V>> extends AMOrderMap<K, V> {

    protected W wrapMap;

    protected AMWrapOrderMap(W wrapMap) {
        this.wrapMap = wrapMap;
    }

    public ImMap<K, V> getMap() {
        return wrapMap;
    }

    public V getValue(int i) {
        return wrapMap.getValue(i);
    }

    public K getKey(int i) {
        return wrapMap.getKey(i);
    }

    public int size() {
        return wrapMap.size();
    }

    public void add(K key, V value) {
        wrapMap.add(key, value);
    }

    public void exclAdd(K key, V value) {
        wrapMap.exclAdd(key, value);
    }

    public void mapValue(int i, V value) {
        wrapMap.mapValue(i, value);
    }
}
