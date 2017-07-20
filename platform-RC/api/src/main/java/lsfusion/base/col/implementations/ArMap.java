package lsfusion.base.col.implementations;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.AMRevMap;
import lsfusion.base.col.implementations.order.ArOrderMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

public class ArMap<K, V> extends AMRevMap<K, V> {
    
    public int size;
    public Object[] keys;
    public Object[] values;

    public ArMap(AddValue<K, V> addValue) {
        super(addValue);

        this.keys = new Object[4];
        this.values = new Object[4];
    }

    public ArMap(int size, Object[] keys, Object[] values) {
        this.size = size;
        this.keys = keys;
        this.values = values;
    }

    public ArMap(ArMap<K, V> map, boolean clone) {
        assert clone;

        size = map.size;
        this.keys = map.keys.clone();
        this.values = map.values.clone();
    }

    public ArMap(ArMap<K, V> map, AddValue<K, V> addValue) {
        super(addValue);

        size = map.size;
        this.keys = map.keys.clone();
        this.values = map.values.clone();
    }

    public ArMap(int size, AddValue<K, V> addValue) {
        super(addValue);

        keys = new Object[size];
        values = new Object[size];
    }

    // на ValueMap
    public ArMap(int size, Object[] keys) {
        this.size = size;
        this.keys = keys;
        this.values = new Object[size];
    }
    public ArMap(ArMap<K, ?> map) {
        this(map.size, map.keys);
    }
    public ArMap(ArSet<K> set) {
        this(set.size, set.array);
    }

    public int size() {
        return size;
    }

    public K getKey(int i) {
        return (K)keys[i];
    }

    public V getValue(int i) {
        return (V)values[i];
    }

    public void mapValue(int i, V value) {
        values[i] = value;
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return new ArMap<>(this);
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new ArMap<>(this);
    }

    protected MExclMap<K, V> copy() {
        return new ArMap<>(this, true);
    }

    private void resize(int length) {
        Object[] newKeys = new Object[length];
        System.arraycopy(keys, 0, newKeys, 0, size);
        Object[] newValues = new Object[length];
        System.arraycopy(values, 0, newValues, 0, size);
        keys = newKeys;
        values = newValues;
    }

    @Override
    public void exclAdd(K key, V value) { // не будем проверять, так как очень большое искажение при профайлинге будет
        if (size >= keys.length) resize(2 * keys.length);
        keys[size] = key;
        values[size++] = value;
    }
    public boolean add(K key, V value) {
        for(int i=0;i<size;i++)
            if(BaseUtils.hashEquals(keys[i], key)) {
                AddValue<K, V> addValue = getAddValue();
                V addedValue = addValue.addValue(key, (V) values[i], value);
                if(addValue.stopWhenNull() && addedValue==null)
                    return false;
                values[i] = addedValue;
                return true;
            }
        exclAdd(key, value);
        return true;
    }

    public ImMap<K, V> immutable() {
        ImMap<K, V> simple = simpleImmutable();
        if(simple!=null)
            return simple;

        if(keys.length > size * SetFact.factorNotResize)
            resize(size);

        if(size < SetFact.useArrayMax)
            return this;

        // упорядочиваем Set
        ArSet.sortArray(size, keys, values);
        return new ArIndexedMap<>(size, keys, values);
    }

    @Override
    public ImRevMap<V, K> reverse() {
        return new ArMap<>(size, values, keys);
    }

    @Override
    public ImOrderMap<K, V> toOrderMap() {
        return new ArOrderMap<>(this);
    }

    @Override
    public ImSet<K> keys() {
        return new ArSet<>(size, keys);
    }

    @Override
    public ImCol<V> values() {
        return new ArCol<>(size, values);
    }

    private ImMap<K, V> merge(int mgSize, Object[] mgKeys, Object[] mgValues, AddValue<K, V> add) {

        int r = 0;
        Object[] rKeys = new Object[size + mgSize];
        Object[] rValues = new Object[size + mgSize];

        boolean[] found = new boolean[mgSize];
        for(int i=0;i<size;i++) {
            K key = (K)keys[i];
            V addedValue = (V)values[i];
            if(!add.exclusive()) {
                for (int j = 0; j < mgSize; j++)
                    if (!found[j] && BaseUtils.hashEquals(key, mgKeys[j])) {
                        found[j] = true;
                        addedValue = add.addValue(key, addedValue, (V) mgValues[j]);
                        if (add.stopWhenNull() && addedValue == null)
                            return null;
                    }
            }
            rKeys[r] = key;
            rValues[r] = addedValue;
            r++;
        }
        for(int j=0;j<mgSize;j++)
            if(!found[j]) {
                rKeys[r] = mgKeys[j];
                rValues[r] = mgValues[j];
                r++;
            }


        if(rKeys.length > r * SetFact.factorNotResize) {
            Object[] newKeys = new Object[r];
            System.arraycopy(rKeys, 0, newKeys, 0, r);
            Object[] newValues = new Object[r];
            System.arraycopy(rValues, 0, newValues, 0, r);
            rKeys = newKeys;
            rValues = newValues;
        }

        if(size < SetFact.useArrayMax)
            return new ArMap<>(r, rKeys, rValues);

        // упорядочиваем Set
        ArSet.sortArray(r, rKeys, rValues);
        return new ArIndexedMap<>(r, rKeys, rValues);
    }

    @Override
    public ImMap<K, V> merge(ImMap<? extends K, ? extends V> map, AddValue<K, V> add) { // важная оптимизация так как ОЧЕНЬ много раз вызывается
        if(map.isEmpty()) return this;

        if(add.reversed() && size < map.size()) return ((ImMap<K, V>)map).merge(this, add.reverse());

        ImMap<K, V> result;
        if(map.size() <= SetFact.useIndexedAddInsteadOfMerge) {
            ArMap<K, V> mResult = new ArMap<>(this, add);
            if(add.exclusive()) {
                mResult.exclAddAll(map);
            } else {
                if (!mResult.addAll(map))
                    return null;
            }
            result = mResult.immutable();
        } else {
            if(map instanceof ArMap) {
                ArMap<K, V> arMap = (ArMap<K, V>) map;
                result = merge(arMap.size, arMap.keys, arMap.values, add);
            } else {
                int mapSize = map.size();
                Object[] mapKeys = new Object[mapSize];
                Object[] mapValues = new Object[mapSize];
                for(int i=0;i<mapSize;i++) {
                    mapKeys[i] = map.getKey(i);
                    mapValues[i] = map.getValue(i);
                }
                ArSet.sortArray(mapSize, mapKeys, mapValues);
                result = merge(mapSize, mapKeys, mapValues, add);
            }
        }

//        assert BaseUtils.nullHashEquals(result, super.merge(map, add));
        return result;
    }

    @Override
    public ImMap<K, V> addExcl(ImMap<? extends K, ? extends V> map) {
        return merge(map, MapFact.<K, V>exclusive());
    }
}
