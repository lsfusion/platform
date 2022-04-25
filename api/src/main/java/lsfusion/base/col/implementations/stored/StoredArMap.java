package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.implementations.abs.AMRevMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

public class StoredArMap<K, V> extends AMRevMap<K, V> {
    private final StoredArray<K> keys;
    private final StoredArray<V> values;
    
    public StoredArMap(StoredArraySerializer serializer, AddValue<K, V> addValue) {
        super(addValue);

        this.keys = new StoredArray<>(serializer);
        this.values = new StoredArray<>(serializer);
    }

    public StoredArMap(int size, K[] keys, V[] values, StoredArraySerializer serializer, AddValue<K, V> addValue) throws StoredArray.StoredArrayCreationException {
        super(addValue);
        this.keys = new StoredArray<>(size, keys, serializer, null);
        this.values = new StoredArray<>(size, values, serializer, null);
    }

    public StoredArMap(int size, K[] keys, V[] values, StoredArraySerializer serializer) throws StoredArray.StoredArrayCreationException {
        this(size, keys, values, serializer, null);
    }

    public StoredArMap(StoredArray<K> keys, StoredArray<V> values) {
        this.keys = keys;
        this.values = values;
        assert keys.size() == values.size();
    }
    
    public StoredArMap(StoredArMap<K, V> map, boolean clone) {
        assert clone;
        this.keys = new StoredArray<>(map.keys);
        this.values = new StoredArray<>(map.values);
    }

    public StoredArMap(StoredArMap<K, V> map, AddValue<K, V> addValue) {
        super(addValue);

        this.keys = new StoredArray<>(map.keys);
        this.values = new StoredArray<>(map.values);
    }

    // на ValueMap
    public StoredArMap(StoredArray<K> keys) {
        this.keys = keys;
        this.values = new StoredArray<>(keys.size(), keys.getSerializer());
    }
    
    public StoredArMap(StoredArMap<K, ?> map) {
        this(map.keys);
    }
    
    public StoredArMap(StoredArSet<K> set) {
        this(set.getStoredArray());
    }


    @Override
    public int size() {
        return keys.size();
    }

    @Override
    public K getKey(int i) {
        return keys.get(i);
    }

    @Override
    public V getValue(int i) {
        return values.get(i);
    }

    public StoredArray<K> getStoredKeys() {
        return keys;
    }

    public StoredArray<V> getStoredValues() {
        return values;
    }

    @Override
    public void mapValue(int i, V value) {
        values.set(i, value);
    }

    @Override
    public <M> ImValueMap<K, M> mapItValues() {
        return new StoredArMap<>(this);
    }

    @Override
    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new StoredArMap<>(this);
    }

    @Override
    public MExclMap<K, V> copy() {
        return new StoredArMap<>(this, true);
    }

    @Override
    public void exclAdd(K key, V value) { // не будем проверять, так как очень большое искажение при профайлинге будет
        keys.append(key);
        values.append(value);
    }

    // very slow O(n) operation 
    @Override
    public boolean add(K key, V value) {
        throw new UnsupportedOperationException();
//        for (int i = 0; i < keys.size(); ++i)
//            if (BaseUtils.hashEquals(keys.get(i), key)) {
//                AddValue<K, V> addValue = getAddValue();
//                V addedValue = addValue.addValue(key, values.get(i), value);
//                if (addValue.stopWhenNull() && addedValue==null)
//                    return false;
//                values.set(i, addedValue);
//                return true;
//            }
//        exclAdd(key, value);
//        return true;
    }

    @Override
    public ImMap<K, V> immutable() {
        return toStoredArIndexedMap();
    }

    public StoredArIndexedMap<K, V> toStoredArIndexedMap() {
        return toStoredArIndexedMap(null);
    }

    public StoredArIndexedMap<K, V> toStoredArIndexedMap(int[] order) {
        keys.sort(values, order);
        return new StoredArIndexedMap<>(keys, values);
    }

    @Override
    public StoredArMap<V, K> reverse() {
        return new StoredArMap<>(values, keys);
    }

    @Override
    public ImOrderMap<K, V> toOrderMap() {
        return new StoredArOrderMap<>(this);
    }

    @Override
    public StoredArSet<K> keys() {
        return new StoredArSet<>(keys);
    }

    @Override
    public StoredArSet<V> values() {
        return new StoredArSet<>(values);
    }

    @Override
    public ImMap<K, V> merge(ImMap<? extends K, ? extends V> map, AddValue<K, V> add) { 
        throw new UnsupportedOperationException();
    }

    @Override
    public ImMap<K, V> addExcl(ImMap<? extends K, ? extends V> map) {
        return merge(map, MapFact.exclusive());
    }
}
