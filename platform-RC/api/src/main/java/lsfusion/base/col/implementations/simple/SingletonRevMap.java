package lsfusion.base.col.implementations.simple;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.ArMap;
import lsfusion.base.col.implementations.abs.ARevMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.AddValue;
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
        return new SingletonRevMap<>(key);
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new SingletonRevMap<>(key);
    }

    @Override
    public ImOrderMap<K, V> toOrderMap() {
        return new SingletonOrderMap<>(this);
    }

    @Override
    public ImRevMap<V, K> reverse() {
        return new SingletonRevMap<>(value, key);
    }

    @Override
    public String toString() {
        return key + " -> " + value;
    }

    public ImMap<K, V> merge(ImMap<? extends K, ? extends V> map, AddValue<K, V> add) { // важная оптимизация так как ОЧЕНЬ много раз вызывается
        if(!add.reversed())
            return super.merge(map, add);

        if (map.isEmpty()) return this;

        if (size() < map.size()) return ((ImMap<K, V>) map).merge(this, add.reverse());

        assert map.size() == 1;
        K mapKey = map.singleKey();
        V mapValue = map.singleValue();

        if(add.exclusive() || !BaseUtils.hashEquals(key, mapKey)) {
            if(2 < SetFact.useArrayMax) {
                return new ArMap<>(2, new Object[]{key, mapKey}, new Object[]{value, mapValue});
            }
        } else {
            V addedValue = add.addValue(key, value, mapValue);
            if (add.stopWhenNull() && addedValue == null)
                return null;
            if(value == addedValue)// оптимизация
                return this;
            if(mapValue == addedValue)// оптимизация
                return (ImMap<K, V>) map;
            return MapFact.singleton(key, addedValue);
        }

        return super.merge(map, add);
    }

    @Override
    public ImMap<K, V> addExcl(ImMap<? extends K, ? extends V> map) {
        return merge(map, MapFact.<K, V>exclusive());
    }
}
