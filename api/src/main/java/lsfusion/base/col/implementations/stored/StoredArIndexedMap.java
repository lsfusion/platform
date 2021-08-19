package lsfusion.base.col.implementations.stored;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.implementations.abs.AMRevMap;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

public class StoredArIndexedMap<K, V> extends AMRevMap<K, V> {

    public StoredArray<K> keys;
    public StoredArray<V> values;

    public StoredArIndexedMap(StoredArraySerializer serializer, K[] keys, V[] values) {
        this.keys = new StoredArray<>(keys, serializer);
        this.values = new StoredArray<>(values, serializer);
        assert keys.length == values.length;
    }

    public StoredArIndexedMap(StoredArray<K> keys, StoredArray<V> values) {
        this.keys = keys;
        this.values = values;
        assert keys.size() == values.size();
    }
    
    public StoredArIndexedMap(StoredArIndexedMap<K, V> map, boolean clone) {
        assert clone;
        this.keys = new StoredArray<>(map.keys);
        this.values = new StoredArray<>(map.values);
    }
    
    private StoredArIndexedMap(StoredArraySerializer serializer, StoredArray<K> keys) {
        this.keys = keys;
        this.values = new StoredArray<>(keys.size(), serializer);
    }

    public StoredArIndexedMap(StoredArIndexedMap<K, ?> map) {
        this(map.keys.getSerializer(), map.keys);
    }
    public StoredArIndexedMap(StoredArIndexedSet<K> set) {
        this(set.array.getSerializer(), set.array);
    }

    public int size() {
        return keys.size();
    }

    public K getKey(int i) {
        return keys.get(i);
    }

    public V getValue(int i) {
        return values.get(i);
    }

    public void mapValue(int i, V value) {
        values.set(i, value);
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return new StoredArIndexedMap<>(this);
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new StoredArIndexedMap<>(this);
    }

    public ImMap<K, V> immutable() {
        return this;
    }

    protected MExclMap<K, V> copy() {
        return new StoredArIndexedMap<>(this, true);
    }

    public static <T> int findIndex(Object key, StoredArray<T> keys) {
        int hash = key.hashCode();

        int low = 0;
        int high = keys.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = keys.get(mid);
            int midHash = midVal.hashCode();

            if (midHash < hash)
                low = mid + 1;
            else if (midHash > hash)
                high = mid - 1;
            else { // hash found
                if (midVal == key || midVal.equals(key))
                    return mid; // key found

                for (int i = mid + 1; i <= high && (midVal = keys.get(i)).hashCode() == hash; i++)
                    if (midVal == key || midVal.equals(key))
                        return i;

                for (int i = mid - 1; i >= low && (midVal = keys.get(i)).hashCode() == hash; i--)
                    if (midVal == key || midVal.equals(key))
                        return i;

                low = mid;
                break;
            }
        }
        return -(low + 1);
    }

    private int findIndex(Object key) {
        return findIndex(key, keys);
    }
    @Override
    public V getObject(Object key) {
        int index = findIndex(key);
        if(index >= 0) {
            return values.get(index);
        }
        return null;
    }

    public boolean add(K key, V value) {
        int index = findIndex(key);
        if (index >= 0) {
            AddValue<K, V> add = getAddValue();
            V addedValue = add.addValue(keys.get(index), values.get(index), value);
            if (add.stopWhenNull() && addedValue == null)
                return false;
            values.set(index, addedValue);
        } else {
            int insert = -index - 1;
            keys.insert(insert, key);
            values.insert(insert, value);
        }
        return true;
    }

    private StoredArIndexedMap<K, V> merge(int mgSize, K[] mgKeys, V[] mgValues, AddValue<K, V> add) {
        StoredArray<K> rKeys = new StoredArray<>(keys.getSerializer());
        StoredArray<V> rValues = new StoredArray<>(values.getSerializer());

        int i=0; int j=0;
        int iHash = keys.get(0).hashCode();
        int jHash = mgKeys[0].hashCode();
        while(i<size() && j<mgSize) {
//            assert iHash == keys[i].hashCode() && jHash == mgKeys[j].hashCode();
            if(iHash<jHash) {
                rKeys.append(keys.get(i));
                rValues.append(values.get(i));
                i++;
                if(i<size())
                    iHash = keys.get(i).hashCode();
            } else if(jHash<iHash) {
                rKeys.append(mgKeys[j]);
                rValues.append(mgValues[j]);
                j++;
                if(j<mgSize)
                    jHash = mgKeys[j].hashCode();
            } else if(iHash == jHash) {
                if(!add.exclusive() && (keys.get(i)==mgKeys[j] || keys.get(i).equals(mgKeys[j]))) { // самый частый случай
                    V addedValue = add.addValue(keys.get(i), values.get(i), mgValues[j]);
                    if(add.stopWhenNull() && addedValue==null)
                        return null;
                    rKeys.append(keys.get(i));
                    rValues.append(addedValue);

                    i++;
                    if(i<size())
                        iHash = keys.get(i).hashCode();
                    j++;
                    if(j<mgSize)
                        jHash = mgKeys[j].hashCode();
                } else {
                    int nj = j; // бежим по второму массиву пока не закончится этот хэш
                    while(true) {
                        nj++;
                        if(nj >= mgSize)
                            break;
                        jHash = mgKeys[nj].hashCode();
                        if(jHash!=iHash)
                            break;
                    }

                    boolean[] found = new boolean[nj-j];
                    int ni = i;
                    while(true) { // бежим по первому массиву пока не закончится хэш и ищем соответствия во втором массиве
                        K key = keys.get(ni);
                        V addedValue = values.get(ni);
                        if(!add.exclusive()) {
                            for (int kj = j; kj < nj; kj++)
                                if (!found[kj - j] && (key == mgKeys[kj] || key.equals(mgKeys[kj]))) {
                                    found[kj - j] = true;
                                    addedValue = add.addValue(key, addedValue, mgValues[kj]);
                                    if (add.stopWhenNull() && addedValue == null)
                                        return null;
                                    break;
                                }
                        }

                        rKeys.append(key);
                        rValues.append(addedValue);

                        ni++;
                        if(ni>=size())
                            break;

                        int kHash = keys.get(ni).hashCode();
                        if(kHash != iHash) {
                            iHash = kHash;
                            break;
                        }
                    }

                    for(int k=j;k<nj;k++)
                        if(!found[k-j]) {
                            rKeys.append(mgKeys[k]);
                            rValues.append(mgValues[k]);
                        }

                    j = nj;
                    i = ni;
                }
            }
        }
        while(i<size()) {
            rKeys.append(keys.get(i));
            rValues.append(values.get(i));
            i++;
        }
        while(j<mgSize) {
            rKeys.append(mgKeys[j]);
            rValues.append(mgValues[j]);
            j++;
        }
//        assert sorted(r, rKeys);
        return new StoredArIndexedMap<>(rKeys, rValues);
    }

    public ImMap<K, V> merge(ImMap<? extends K, ? extends V> map, AddValue<K, V> add) { // важная оптимизация так как ОЧЕНЬ много раз вызывается
        throw new UnsupportedOperationException();        
//        if(map.isEmpty()) return this;
//        StoredArIndexedMap<K, V> result;
//        
//        if(map.size() <= SetFact.useIndexedAddInsteadOfMerge) {
//            result = new StoredArIndexedMap<>(this, true);
//            if(add.exclusive())
//                result.exclAddAll(map);
//            else {
//                if (!result.addAll(map))
//                    return null;
//            }
//        } else {
//            if(map instanceof StoredArIndexedMap) {
//                StoredArIndexedMap<K, V> arMap = (StoredArIndexedMap<K, V>) map;
//                result = merge(arMap.size, arMap.keys, arMap.values, add);
//            } else {
//                int mapSize = map.size();
//                K[] mapKeys = (K[])new Object[mapSize];
//                V[] mapValues = (V[])new Object[mapSize];
//                for(int i=0;i<mapSize;i++) {
//                    mapKeys[i] = map.getKey(i);
//                    mapValues[i] = map.getValue(i);
//                }
//                ArSet.sortArray(mapSize, mapKeys, mapValues);
//                result = merge(mapSize, mapKeys, mapValues, add);
//            }
//        }
//
////        assert BaseUtils.hashEquals(result, super.merge(map, add));
//        return result;
    }

    @Override
    public ImMap<K, V> addExcl(ImMap<? extends K, ? extends V> map) {
        return merge(map, MapFact.exclusive());
    }

    @Override
    public void keep(K key, V value) {
        assert size() == 0 || keys.get(size() - 1).hashCode() <= key.hashCode();
        keys.append(key);
        values.append(value);
    }

    @Override
    public ImOrderMap<K, V> toOrderMap() {
        // todo [dale]: 
        throw new UnsupportedOperationException();
//        return new StoredArOrderIndexedMap<>(this, ArSet.genOrder(size()));
    }

    @Override
    public ImSet<K> keys() {
        return new StoredArIndexedSet<>(keys);
    }

    @Override
    public ImCol<V> values() {
        return new StoredArIndexedSet<>(values);
    }

    // копия с merge
    protected boolean twins(StoredArray<K> twKeys, StoredArray<V> twValues) {

        int i=0;
        int hash = keys.get(0).hashCode();
        int twHash = twKeys.get(0).hashCode();
        while(i<size()) {
//            assert iHash == keys[i].hashCode() && jHash == mgKeys[j].hashCode();
            if(hash<twHash)
                return false;
            else if(twHash<hash)
                return false;
            else if(hash == twHash) {
                if(keys.get(i)==twKeys.get(i) || keys.get(i).equals(twKeys.get(i))) { // самый частый случай
                    if(!BaseUtils.nullHashEquals(values.get(i), twValues.get(i)))
                        return false;

                    i++;
                    if(i<size()) {
                        hash = keys.get(i).hashCode();
                        twHash = twKeys.get(i).hashCode();
                    }
                } else {
                    int ntw = i; // бежим по второму массиву пока не закончится этот хэш
                    while(true) {
                        ntw++;
                        if(ntw >= size())
                            break;
                        twHash = twKeys.get(ntw).hashCode();
                        if(twHash!=hash)
                            break;
                    }

                    boolean[] found = new boolean[ntw-i];
                    int ni = i;
                    while(true) { // бежим по первому массиву пока не закончится хэш и ищем соответствия во втором массиве
                        K key = keys.get(ni);
                        V addedValue = values.get(ni);
                        boolean founded = false;
                        for(int ktw = i; ktw < ntw; ktw++)
                            if(!found[ktw-i] && (key == twKeys.get(ktw) || key.equals(twKeys.get(ktw)))) {
                                if(!BaseUtils.nullHashEquals(addedValue, twValues.get(ktw)))
                                    return false;

                                found[ktw-i] = true;
                                founded = true;
                                break;
                            }

                        if(!founded)
                            return false;

                        ni++;
                        if(ni>=size())
                            break;

                        int kHash = keys.get(ni).hashCode();
                        if(kHash != hash) {
                            if(ni<ntw)
                                return false;
                            hash = kHash;
                            break;
                        }
                    }

                    assert ni==ntw;

                    i = ni;
                }
            }
        }

        return true;
    }
    @Override
    protected boolean twins(ImMap<K, V> map) { // assert что size'ы совпадает
        throw new UnsupportedOperationException();
//
//        if(map instanceof StoredArIndexedMap) {
//            StoredArIndexedMap<K, V> arMap = ((StoredArIndexedMap<K, V>)map);
//            return twins(arMap.keys, arMap.values);
//        }
//
//        return super.twins(map);
    }
}
