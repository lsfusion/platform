package platform.base.col.implementations.abs;

import platform.base.col.MapFact;
import platform.base.col.implementations.abs.ARevMap;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.*;
import platform.base.col.interfaces.mutable.add.MAddExclMap;
import platform.base.col.interfaces.mutable.add.MAddMap;
import platform.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import platform.base.col.interfaces.mutable.mapvalue.ImValueMap;

public abstract class AMRevMap<K, V> extends ARevMap<K, V> implements MMap<K, V>, MExclMap<K, V>, MRevMap<K, V>, ImRevValueMap<K, V>, ImValueMap<K, V>, MAddMap<K, V>, MAddExclMap<K, V>, MFilterMap<K, V>, MFilterRevMap<K, V> {

    protected Object data; // если immutable то reverse, иначе addValue (см. AMRevMap)
    public ImRevMap<V, K> reverse() {
        if(data==null) {
            data = super.reverse();
            ((AMRevMap<V, K>)data).data = this;
        }
        return (ImRevMap<V, K>)data;
    }

    protected final AddValue<K, V> getAddValue() {
        return (AddValue<K, V>)data;
    }

    public AMRevMap() {
    }

    public AMRevMap(AddValue<K, V> addValue) {
        this.data = addValue;
    }

    protected final ImMap<K, V> simpleImmutable() {
        this.data = null;

        if(size()==0)
            return MapFact.EMPTY();
        if(size()==1)
            return MapFact.singleton(singleKey(), singleValue());

        return null;
    }

    public V getMapValue(int i) {
        return getValue(i);
    }

    public K getMapKey(int i) {
        return getKey(i);
    }

    public int mapSize() {
        return size();
    }

    public void exclAdd(K key, V value) {
        assert !containsKey(key);
        boolean added = add(key, value);
        assert added;
    }

    public void exclAddAll(ImMap<? extends K, ? extends V> map) {
        for(int i=0,size=map.size();i<size;i++)
            exclAdd(map.getKey(i), map.getValue(i));
    }

    public boolean addAll(ImMap<? extends K, ? extends V> map) {
        for (int i = 0, size = map.size(); i < size; i++)
            if(!add(map.getKey(i), map.getValue(i)))
                return false;
        return true;
    }

    public void revAdd(K key, V value) {
        assert value!=null && (size() > 100 || !containsValue(value)); // тормознутый assertion поэтому так
        exclAdd(key, value);
    }

    public void revAddAll(ImRevMap<? extends K, ? extends V> map) {
        for(int i=0,size=map.size();i<size;i++)
            revAdd(map.getKey(i), map.getValue(i));
    }

    public void revKeep(K key, V value) {
        keep(key, value);
    }

    public void keep(K key, V value) {
        exclAdd(key, value);
    }

    public ImRevMap<K, V> immutableRev() {
        return (ImRevMap<K, V>)immutable();
    }

    public ImRevMap<K, V> immutableValueRev() {
        assert getAddValue() == null;
        return this;
    }

    public ImMap<K, V> immutableValue() {
        assert getAddValue() == null;
        return this;
    }
}
