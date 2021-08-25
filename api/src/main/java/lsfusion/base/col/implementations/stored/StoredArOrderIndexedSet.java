package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.abs.AMOrderSet;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

public class StoredArOrderIndexedSet<K> extends AMOrderSet<K> {
    public StoredArIndexedSet<K> arSet; // для дружественных классов
    // todo [dale]: can be heavily optimized 
    public StoredArray<Integer> order;

    public StoredArOrderIndexedSet(StoredArraySerializer serializer, int size) {
        arSet = new StoredArIndexedSet<>(serializer, size);
        order = new StoredArray<>(size, serializer);
    }

    public StoredArOrderIndexedSet(StoredArIndexedSet<K> arSet, StoredArray<Integer> order) {
        this.arSet = arSet;
        this.order = order;
    }

    public ImSet<K> getSet() {
        return arSet;
    }

    public int size() {
        return arSet.size();
    }

    public K get(int i) {
        return arSet.get(order.get(i));
    }

    public boolean add(K key) {
        throw new UnsupportedOperationException();
    }

    public void exclAdd(K key) {
        throw new UnsupportedOperationException();
    }
    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new StoredArOrderIndexedMap<>(this);
    }

    private class RevMap<V> implements ImRevValueMap<K, V> {
        private StoredArIndexedMap<K, V> result = new StoredArIndexedMap<>(arSet);

        public void mapValue(int i, V value) {
            result.mapValue(order.get(i), value);
        }

        public ImRevMap<K, V> immutableValueRev() {
            return result.immutableValueRev();
        }

        public V getMapValue(int i) {
            return result.getMapValue(order.get(i));
        }

        public K getMapKey(int i) {
            return result.getMapKey(order.get(i));
        }

        public int mapSize() {
            return result.mapSize();
        }
    }
    public <M> ImRevValueMap<K, M> mapItOrderRevValues() {
        return new RevMap<>();
    }

    public ImOrderSet<K> immutableOrder() {
        return this;
    }
}
