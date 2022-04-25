package lsfusion.base.col.implementations;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.AMRevMap;
import lsfusion.base.col.implementations.order.ArOrderIndexedMap;
import lsfusion.base.col.implementations.stored.StoredArIndexedMap;
import lsfusion.base.col.implementations.stored.StoredArray;
import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static lsfusion.base.col.implementations.stored.StoredImplementationsPolicy.LIMIT;
import static lsfusion.base.col.implementations.stored.StoredImplementationsPolicy.STORED_FLAG;

public class ArIndexedMap<K, V> extends AMRevMap<K, V> {

    private int size;
    private Object[] keys;
    private Object[] values;

    public ArIndexedMap(AddValue<K, V> addValue) {
        super(addValue);

        this.keys = new Object[1];
        this.values = new Object[1];
    }

    public ArIndexedMap(int size, Object[] keys, Object[] values) {
        setSizeKeysAndValues(size, keys, values);
        assert keys.length == values.length;
    }

    public ArIndexedMap(ArIndexedMap<K, V> map, boolean clone) {
        this(map, null);
        assert clone;
    }

    public ArIndexedMap(ArIndexedMap<K, V> map, AddValue<K, V> addValue) {
        super(addValue);
        if (map.isStored()) {
            switchToStored(new StoredArIndexedMap<>(map.stored(), true));
        } else if (needToBeStored(map)) {
            switchToStored(map.size(), map.keys, map.values);
        }
        if (!isStored()) {
            cloneKeysAndValues(map);
        }
    }

    public ArIndexedMap(int size, AddValue<K, V> addValue) {
        super(addValue);

        keys = new Object[size];
        values = new Object[size];
    }

    // на ValueMap
    public ArIndexedMap(int size, Object[] keys) {
        setSizeAndKeys(size, keys);
    }

    public ArIndexedMap(ArIndexedMap<K, ?> map) {
        if (!map.isStored()) {
            setSizeAndKeys(map.size, map.keys);
        } else {
            switchToStored(new StoredArIndexedMap<>(map.stored()));
        }
    }

    public ArIndexedMap(ArIndexedSet<K> set) {
        if (!set.isStored()) {
            setSizeAndKeys(set.size(), set.getArray());
        } else {
            switchToStored(new StoredArIndexedMap<>(set.stored()));
        }
    }

    public ArIndexedMap(StoredArray<K> keys, StoredArray<V> values) {
        switchToStored(new StoredArIndexedMap<>(keys, values));
    }

    private void setSizeAndKeys(int size, Object[] keys) {
        this.size = size;
        this.keys = keys;
        this.values = new Object[keys.length];
    }

    private void setSizeKeysAndValues(int size, Object[] keys, Object[] values) {
        this.size = size;
        this.keys = keys;
        this.values = values;
    }

    private void cloneKeysAndValues(ArIndexedMap<K, V> map) {
        this.size = map.size;
        this.keys = map.keys.clone();
        this.values = map.values.clone();
    }

    public int size() {
        if (!isStored()) {
            return size;
        } else {
            return stored().size();
        }
    }

    public K getKey(int i) {
        if (!isStored()) {
            return (K) keys[i];
        } else {
            return stored().getKey(i);
        }
    }

    public V getValue(int i) {
        if (!isStored()) {
            return (V) values[i];
        } else {
            return stored().getValue(i);
        }
    }

    public void mapValue(int i, V value) {
        if (!isStored()) {
            values[i] = value;
        } else {
            stored().mapValue(i, value);
        }
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return new ArIndexedMap<>(this);
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new ArIndexedMap<>(this);
    }

    public ImMap<K, V> immutable() {
        ImMap<K, V> simple = simpleImmutable();
        if (simple != null)
            return simple;

        if (!isStored()) {
            if (needToBeStored(this) && switchToStored(size, keys, values)) {
                return stored().immutable();
            }

            if (keys.length > size * SetFact.factorNotResize)
                resize(size);

            if (size < SetFact.useArrayMax)
                return new ArMap<>(size, keys, values);
        }
        return this;
    }

    protected MExclMap<K, V> copy() {
        return new ArIndexedMap<>(this, true);
    }

    public static int findIndex(Object key, int size, Object[] keys) {
        int hash = key.hashCode();

        int low = 0;
        int high = size - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Object midVal = keys[mid];
            int midHash = midVal.hashCode();

            if (midHash < hash)
                low = mid + 1;
            else if (midHash > hash)
                high = mid - 1;
            else { // hash found
                if(midVal == key || midVal.equals(key))
                    return mid; // key found

                for(int i = mid + 1 ; i<=high && (midVal = keys[i]).hashCode() == hash ; i++)
                    if(midVal == key || midVal.equals(key))
                        return i;

                for(int i = mid - 1 ; i>=low && (midVal = keys[i]).hashCode() == hash ; i--)
                    if(midVal == key || midVal.equals(key))
                        return i;

                low = mid;
                break;
            }
        }
        return -(low+1);
    }

    private int findIndex(Object key) {
        return findIndex(key, size, keys);
    }
    @Override
    public V getObject(Object key) {
        if (!isStored()) {
            int index = findIndex(key);
            if (index >= 0)
                return (V) values[index];
            return null;
        } else {
            return stored().getObject(key); 
        }
    }

    private void resize(int length) {
        Object[] newKeys = new Object[length];
        System.arraycopy(keys, 0, newKeys, 0, size);
        keys = newKeys;
        Object[] newValues = new Object[length];
        System.arraycopy(values, 0, newValues, 0, size);
        values = newValues;
    }

    public boolean add(K key, V value) {
        if (!isStored()) {
            if (size >= keys.length) resize(2 * keys.length);

            int index = findIndex(key);
            if (index >= 0) {
                AddValue<K, V> add = getAddValue();
                V addedValue = add.addValue((K) keys[index], (V) values[index], value);
                if (add.stopWhenNull() && addedValue == null)
                    return false;
                values[index] = addedValue;
                return true;
            }

            int insert = (-index - 1);
            System.arraycopy(keys, insert, keys, insert + 1, size - insert);
            System.arraycopy(values, insert, values, insert + 1, size - insert);
            keys[insert] = key;
            values[insert] = value;
            size++;
            switchToStoredIfNeeded(size-1, size);
            return true;
        } else {
            return stored().add(key, value);
        }
    }
    
    private ArIndexedMap<K, V> merge(int mgSize, Object[] mgKeys, Object[] mgValues, AddValue<K, V> add) {
        int r=0;
        Object[] rKeys = new Object[size + mgSize];
        Object[] rValues = new Object[size + mgSize];

        int i=0; int j=0;
        int iHash = keys[0].hashCode();
        int jHash = mgKeys[0].hashCode();
        while(i<size && j<mgSize) {
//            assert iHash == keys[i].hashCode() && jHash == mgKeys[j].hashCode();
            if(iHash<jHash) {
                rKeys[r] = keys[i];
                rValues[r] = values[i];
                r++;
                i++;
                if(i<size)
                    iHash = keys[i].hashCode();
            } else if(jHash<iHash) {
                rKeys[r] = mgKeys[j];
                rValues[r] = mgValues[j];
                r++;
                j++;
                if(j<mgSize)
                    jHash = mgKeys[j].hashCode();
            } else if(iHash == jHash) {
                if(!add.exclusive() && (keys[i]==mgKeys[j] || keys[i].equals(mgKeys[j]))) { // самый частый случай
                    V addedValue = add.addValue((K)keys[i], (V)values[i], (V)mgValues[j]);
                    if(add.stopWhenNull() && addedValue==null)
                        return null;
                    rKeys[r] = keys[i];
                    rValues[r] = addedValue;
                    r++;

                    i++;
                    if(i<size)
                        iHash = keys[i].hashCode();
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
                        K key = (K)keys[ni];
                        V addedValue = (V)values[ni];
                        if(!add.exclusive()) {
                            for (int kj = j; kj < nj; kj++)
                                if (!found[kj - j] && (key == mgKeys[kj] || key.equals(mgKeys[kj]))) {
                                    found[kj - j] = true;
                                    addedValue = add.addValue(key, addedValue, (V) mgValues[kj]);
                                    if (add.stopWhenNull() && addedValue == null)
                                        return null;
                                    break;
                                }
                        }

                        rKeys[r] = key;
                        rValues[r] = addedValue;
                        r++;

                        ni++;
                        if(ni>=size)
                            break;

                        int kHash = keys[ni].hashCode();
                        if(kHash != iHash) {
                            iHash = kHash;
                            break;
                        }
                    }

                    for(int k=j;k<nj;k++)
                        if(!found[k-j]) {
                            rKeys[r] = mgKeys[k];
                            rValues[r] = mgValues[k];
                            r++;
                        }

                    j = nj;
                    i = ni;
                }
            }
        }
        while(i<size) {
            rKeys[r] = keys[i];
            rValues[r] = values[i];
            r++;
            i++;
        }
        while(j<mgSize) {
            rKeys[r] = mgKeys[j];
            rValues[r] = mgValues[j];
            r++;
            j++;
        }

        if(rKeys.length > r * SetFact.factorNotResize) {
            Object[] newKeys = new Object[r];
            System.arraycopy(rKeys, 0, newKeys, 0, r);
            Object[] newValues = new Object[r];
            System.arraycopy(rValues, 0, newValues, 0, r);
            rKeys = newKeys;
            rValues = newValues;
        }

//        assert sorted(r, rKeys);
        return new ArIndexedMap<>(r, rKeys, rValues);
    }
    
    private static boolean sorted(int size, Object[] array) {
        for(int i=0;i<size-1;i++)
            if(array[i].hashCode() > array[i].hashCode())
                return false;
        return true;
    }

    @Override
    public ImMap<K, V> merge(ImMap<? extends K, ? extends V> map, AddValue<K, V> add) { // важная оптимизация так как ОЧЕНЬ много раз вызывается
        if (map.isEmpty()) return this;
        
        if (!isStored()) {

            if (add.reversed() && size < map.size()) return ((ImMap<K, V>) map).merge(this, add.reverse());

            ImMap<K, V> result;
            if (map.size() <= SetFact.useIndexedAddInsteadOfMerge) {
                ArIndexedMap<K, V> mResult = new ArIndexedMap<>(this, add);
                if (add.exclusive())
                    mResult.exclAddAll(map);
                else {
                    if (!mResult.addAll(map))
                        return null;
                }
                result = mResult.immutable();
            } else {
                if (map instanceof ArIndexedMap) {
                    ArIndexedMap<K, V> arMap = (ArIndexedMap<K, V>) map;
                    result = merge(arMap.size, arMap.keys, arMap.values, add);
                } else {
                    int mapSize = map.size();
                    Object[] mapKeys = new Object[mapSize];
                    Object[] mapValues = new Object[mapSize];
                    for (int i = 0; i < mapSize; i++) {
                        mapKeys[i] = map.getKey(i);
                        mapValues[i] = map.getValue(i);
                    }
                    ArSet.sortArray(mapSize, mapKeys, mapValues);
                    result = merge(mapSize, mapKeys, mapValues, add);
                }
            }

            //        assert BaseUtils.hashEquals(result, super.merge(map, add));
            return result;
        } else {
            return stored().merge(map, add);
        }
    }

    @Override
    public ImMap<K, V> addExcl(ImMap<? extends K, ? extends V> map) {
        if (!isStored()) {
            return merge(map, MapFact.exclusive());
        } else {
            return stored().addExcl(map);
        }
    }

    @Override
    public void keep(K key, V value) {
        if (!isStored()) {
            assert size == 0 || keys[size - 1].hashCode() <= key.hashCode();
            keys[size] = key;
            values[size++] = value;
            switchToStoredIfNeeded(size-1, size);
        } else {
            stored().keep(key, value);
        }
    }

    @Override
    public ImOrderMap<K, V> toOrderMap() {
        return new ArOrderIndexedMap<>(this, ArSet.genOrder(size()));
    }

    @Override
    public ImSet<K> keys() {
        if (!isStored()) {
            return new ArIndexedSet<>(size, keys);
        } else {
            return stored().keys();
        }
    }

    @Override
    public ImCol<V> values() {
        if (!isStored()) {
            return new ArCol<>(size, values);
        } else {
            return stored().values();
        }
    }

    // копия с merge
    protected boolean twins(Object[] twKeys, Object[] twValues) {

        int i=0;
        int hash = keys[0].hashCode();
        int twHash = twKeys[0].hashCode();
        while(i<size) {
//            assert iHash == keys[i].hashCode() && jHash == mgKeys[j].hashCode();
            if(hash<twHash)
                return false;
            else if(twHash<hash)
                return false;
            else if(hash == twHash) {
                if(keys[i]==twKeys[i] || keys[i].equals(twKeys[i])) { // самый частый случай
                    if(!BaseUtils.nullHashEquals(values[i], twValues[i]))
                        return false;

                    i++;
                    if(i<size) {
                        hash = keys[i].hashCode();
                        twHash = twKeys[i].hashCode();
                    }
                } else {
                    int ntw = i; // бежим по второму массиву пока не закончится этот хэш
                    while(true) {
                        ntw++;
                        if(ntw >= size)
                            break;
                        twHash = twKeys[ntw].hashCode();
                        if(twHash!=hash)
                            break;
                    }

                    boolean[] found = new boolean[ntw-i];
                    int ni = i;
                    while(true) { // бежим по первому массиву пока не закончится хэш и ищем соответствия во втором массиве
                        K key = (K)keys[ni];
                        V addedValue = (V)values[ni];
                        boolean founded = false;
                        for(int ktw = i; ktw < ntw; ktw++)
                            if(!found[ktw-i] && (key == twKeys[ktw] || key.equals(twKeys[ktw]))) {
                                if(!BaseUtils.nullHashEquals(addedValue, twValues[ktw]))
                                    return false;

                                found[ktw-i] = true;
                                founded = true;
                                break;
                            }

                        if(!founded)
                            return false;

                        ni++;
                        if(ni>=size)
                            break;

                        int kHash = keys[ni].hashCode();
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
        if (!isStored()) {
            if (map instanceof ArIndexedMap) {
                ArIndexedMap<K, V> arMap = ((ArIndexedMap<K, V>) map);
                return twins(arMap.keys, arMap.values);
            }

            return super.twins(map);
        } else {
            return stored().twins(map);
        }
    }

    public void shrink() {
        if (!isStored() && keys.length > size * SetFact.factorNotResize) {
            Object[] newKeys = new Object[size];
            System.arraycopy(keys, 0, newKeys, 0, size);
            keys = newKeys;
            Object[] newValues = new Object[size];
            System.arraycopy(values, 0, newValues, 0, size);
            values = newValues;
        }
    }
        
    public Object[] getKeys() {
        if (!isStored()) {
            return keys;
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    public Object[] getValues() {
        if (!isStored()) {
            return values;
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    public StoredArray<K> getStoredKeys() {
        if (isStored()) {
            return (StoredArray<K>) keys[0];
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    public StoredArray<V> getStoredValues() {
        if (isStored()) {
            return (StoredArray<V>) values[0];
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        ArIndexedMap<?, ?> map = (ArIndexedMap<?, ?>) o;
        assert !map.isStored();
        serializer.serialize(map.size, outStream);
        ArCol.serializeArray(map.keys, serializer, outStream);
        ArCol.serializeArray(map.values, serializer, outStream);
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        int size = (int)serializer.deserialize(inStream);
        Object[] keys = ArCol.deserializeArray(inStream, serializer);
        Object[] values = ArCol.deserializeArray(inStream, serializer);
        return new ArIndexedMap<>(size, keys, values);
    }

    public boolean isStored() {
        return size == STORED_FLAG;
    }

    private StoredArIndexedMap<K, V> stored() {
        return (StoredArIndexedMap<K, V>) keys[0];
    }

    private void switchToStoredIfNeeded(int oldSize, int newSize) {
        if (oldSize <= LIMIT && newSize > LIMIT && needToBeStored(this)) {
            switchToStored(size, keys, values);
        }
    }

    private static boolean needToBeStored(ArIndexedMap<?, ?> map) {
        return !map.isStored() && map.size() > LIMIT && canBeStored(map);
    }

    private static boolean canBeStored(ArIndexedMap<?, ?> map) {
        return StoredArraySerializer.getInstance().canBeSerialized(map.getKey(0))
            && StoredArraySerializer.getInstance().canBeSerialized(map.getValue(0));
    }

    private boolean switchToStored(int size, Object[] keys, Object[] values) {
        try {
            AddValue<K, V> addValue = (data instanceof AddValue ? (AddValue<K, V>) data : null);
            StoredArIndexedMap<K, V> storedMap =
                    new StoredArIndexedMap<>(size, (K[]) keys, (V[]) values, StoredArraySerializer.getInstance(), addValue);
            switchToStored(storedMap);
            return true;
        } catch (StoredArray.StoredArrayCreationException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void switchToStored(StoredArIndexedMap<K, V> storedMap) {
        this.keys = new Object[]{storedMap};
        this.size = STORED_FLAG;
    }
}
