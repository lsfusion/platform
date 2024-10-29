package lsfusion.base.col.implementations.abs;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

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

    protected abstract MExclMap<K, V> copy();

    public ImMap<K, V> immutableCopy() {
        return copy().immutable();
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

    @Override
    public void exclAddAll(ImSet<? extends K> set, V value) {
        for(int i=0,size=set.size();i<size;i++)
            exclAdd(set.get(i), value);
    }

    public boolean addAll(ImMap<? extends K, ? extends V> map) {
        for (int i = 0, size = map.size(); i < size; i++)
            if(!add(map.getKey(i), map.getValue(i)))
                return false;
        return true;
    }

    public boolean addAll(ImSet<? extends K> set, V value) {
        for (int i = 0, size = set.size(); i < size; i++)
            if(!add(set.get(i), value))
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
