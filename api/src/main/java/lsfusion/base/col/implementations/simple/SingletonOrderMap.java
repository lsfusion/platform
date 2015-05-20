package lsfusion.base.col.implementations.simple;

import lsfusion.base.col.implementations.abs.AOrderMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;

public class SingletonOrderMap<K, V> extends AOrderMap<K, V> implements ImOrderValueMap<K, V> {
    
    private SingletonRevMap<K, V> revMap;

    public SingletonOrderMap(K key) {
        revMap = new SingletonRevMap<K, V>(key);
    }

    public SingletonOrderMap(SingletonRevMap<K, V> revMap) {
        this.revMap = revMap;
    }

    public SingletonOrderMap(K key, V value) {
        revMap = new SingletonRevMap<K, V>(key, value);
    }

    public int size() {
        return 1;
    }

    public K getKey(int i) {
        assert i==0;
        return revMap.getKey(0);
    }

    public V getValue(int i) {
        assert i==0;
        return revMap.getValue(0);
    }

    public ImMap<K, V> getMap() {
        return revMap;
    }

    public void mapValue(int i, V value) {
        assert i==0;
        revMap.mapValue(0, value);
    }

    public ImOrderMap<K, V> immutableValueOrder() {
        return this;
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new SingletonOrderMap<K, M>(revMap.getKey(0));
    }

    @Override
    public String toString() {
        return revMap.toString();
    }
}
