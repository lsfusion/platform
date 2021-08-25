package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.abs.AMOrderMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;

public class StoredArOrderIndexedMap<K, V> extends AMOrderMap<K, V> {

    public StoredArIndexedMap<K, V> arMap;
    private StoredArray<Integer> order;

    public StoredArOrderIndexedMap(StoredArOrderIndexedMap<K, V> orderMap, AddValue<K, V> addValue) {
        arMap = new StoredArIndexedMap<>(orderMap.arMap, addValue);
        order = new StoredArray<>(orderMap.order);
    }

    public StoredArOrderIndexedMap(StoredArIndexedMap<K, V> arMap, StoredArray<Integer> order) {
        this.arMap = arMap;
        this.order = order;
    }

    // ImValueMap
    public StoredArOrderIndexedMap(StoredArOrderIndexedMap<K, ?> orderMap) {
        arMap = new StoredArIndexedMap<>(orderMap.arMap);
        order = new StoredArray<>(orderMap.order);
    }

    public StoredArOrderIndexedMap(StoredArOrderIndexedSet<K> orderSet) {
        arMap = new StoredArIndexedMap<>(orderSet.arSet);
        order = new StoredArray<>(orderSet.order);
    }


    public StoredArOrderIndexedMap(StoredArOrderIndexedMap<K, V> orderMap, boolean clone) {
        arMap = new StoredArIndexedMap<>(orderMap.arMap, clone);
        order = new StoredArray<>(orderMap.order);
        assert clone;
    }

    public MOrderExclMap<K, V> orderCopy() {
        return new StoredArOrderIndexedMap<>(this, true);
    }

    public ImMap<K, V> getMap() {
        return arMap;
    }

    public V getValue(int i) {
        return arMap.getValue(order.get(i));
    }

    public K getKey(int i) {
        return arMap.getKey(order.get(i));
    }

    public int size() {
        return arMap.size();
    }

    public void add(K key, V value) {
        throw new UnsupportedOperationException();
    }

    public void exclAdd(K key, V value) {
        throw new UnsupportedOperationException();
    }

    public void mapValue(int i, V value) {
        arMap.mapValue(order.get(i), value);
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new StoredArOrderIndexedMap<>(this);
    }

    public ImOrderMap<K, V> immutableOrder() {
        return this;
    }

    @Override
    public ImOrderSet<K> keyOrderSet() {
        return new StoredArOrderIndexedSet<>(new StoredArIndexedSet<>(arMap.keys), order);
    }
    
}
