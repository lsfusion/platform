package lsfusion.base.col.implementations.simple;

import lsfusion.base.col.implementations.abs.ARevMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

public class SingletonRevMap<K, V> extends ARevMap<K, V> implements ImValueMap<K, V>, ImRevValueMap<K, V> {
    
    private K key;
    private V value;

    public SingletonRevMap(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public int size() {
        return 1;
    }

    public K getKey(int i) {
        assert i==0;
        return key;
    }

    public V getValue(int i) {
        assert i==0;
        return value;
    }

    public SingletonRevMap(K key) {
        this.key = key;
    }

    public ImRevMap<K, V> immutableValueRev() {
        return this;
    }

    public ImMap<K, V> immutableValue() {
        return this;
    }

    public void mapValue(int i, V value) {
        assert i==0;
        this.value = value;
    }

    public V getMapValue(int i) {
        assert i==0;
        return this.value;
    }

    public K getMapKey(int i) {
        assert i==0;
        return this.key;
    }

    public int mapSize() {
        return 1;
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return new SingletonRevMap<K, M>(key);
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new SingletonRevMap<K, M>(key);
    }

    @Override
    public ImOrderMap<K, V> toOrderMap() {
        return new SingletonOrderMap<K, V>(this);
    }

    @Override
    public ImRevMap<V, K> reverse() {
        return new SingletonRevMap<V, K>(value, key);
    }
}
