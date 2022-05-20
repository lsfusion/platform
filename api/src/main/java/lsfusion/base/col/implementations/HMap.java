package lsfusion.base.col.implementations;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.AMRevMap;
import lsfusion.base.col.implementations.order.HOrderMap;
import lsfusion.base.col.implementations.stored.StoredArray;
import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.implementations.stored.StoredHMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static lsfusion.base.col.implementations.stored.StoredArray.isStoredArraysEnabled;
import static lsfusion.base.col.implementations.stored.StoredImplementationsPolicy.LIMIT;
import static lsfusion.base.col.implementations.stored.StoredImplementationsPolicy.STORED_FLAG;

// дублируем HSet
public class HMap<K, V> extends AMRevMap<K, V> {
    private int size;
    private Object[] table;
    private Object[] vtable;

    private int[] indexes; // номера в таблице

    private final static float loadFactor = 0.3f;

    public HMap(AddValue<K, V> addValue) {
        super(addValue);

        table = new Object[8];
        vtable = new Object[8];

        indexes = new int[(int) (table.length * loadFactor)];
    }

    public HMap(HMap<? extends K, ? extends V> map, boolean clone) {
        this(map, null);
        assert clone;
    }

    public HMap(HMap<? extends K, ? extends V> map, AddValue<K, V> addValue) {
        super(addValue);
        if (map.isStored()) {
            switchToStored(new StoredHMap<>(map.stored(), true));
        } else if (needToBeStored(map)) {
            switchToStored(map.size, map.table, map.vtable, map.indexes);
        }
        if (!isStored()) {
            setData(map.size, map.table.clone(), map.vtable.clone(), map.indexes.clone());
        }
    }

    public HMap(int size, Object[] table, Object[] vtable, int[] indexes) {
        setData(size, table, vtable, indexes);
    }

    public HMap(int size, StoredArray<K> table, StoredArray<V> vtable, int[] indexes) {
        switchToStored(new StoredHMap<>(size, table, vtable, indexes, null));
    }

    public HMap(int size, AddValue<K, V> addValue) {
        super(addValue);

        int initialCapacity = (int)(size/loadFactor) + 1;

        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;

        table = new Object[capacity];
        vtable = new Object[capacity];

        indexes = new int[size];
    }

    public HMap(HMap<K, ?> map) {
        if (!map.isStored()) {
            setData(map.size, map.table, map.indexes);
        } else {
            switchToStored(new StoredHMap<>(map.stored()));
        }
    }

    public HMap(HSet<K> set) {
        if (!set.isStored()) {
            setData(set.size(), set.getTable(), new Object[table.length], set.getIndexes());
        } else {
            switchToStored(new StoredHMap<>(set.stored()));
        }
    }

    private void setData(int size, Object[] table, int[] indexes) {
        setData(size, table, new Object[table.length], indexes);
    }

    private void setData(int size, Object[] table, Object[] vtable, int[] indexes) {
        this.size = size;
        this.table = table;
        this.vtable = vtable;
        this.indexes = indexes;
    }

    public int size() {
        if (!isStored()) {
            return size;
        } else {
            return stored().size();
        }
    }

    public K getKey(int i) {
        assert i<size;
        if (!isStored()) {
            return (K) table[indexes[i]];
        } else {
            return stored().getKey(i);
        }
    }

    public V getValue(int i) {
        assert i<size;
        if (!isStored()) {
            return (V) vtable[indexes[i]];
        } else {
            return stored().getValue(i);
        }
    }

    private void resize(int length) {
        int[] newIndexes = new int[(int) (length * loadFactor) + 1];

        Object[] newTable = new Object[length];
        Object[] newVTable = new Object[length];
        for (int i = 0; i < size; i++) {
            Object key = table[indexes[i]];
            int newHash = MapFact.colHash(key.hashCode()) & (length - 1);
            while (newTable[newHash] != null) newHash = (newHash == length - 1 ? 0 : newHash + 1);

            newTable[newHash] = key;
            newVTable[newHash] = vtable[indexes[i]];

            newIndexes[i] = newHash;
        }

        table = newTable;
        vtable = newVTable;

        indexes = newIndexes;
    }

    public boolean add(K key, V value) {
        if (!isStored()) {
            if (size >= indexes.length) resize(2 * table.length);
            assert !(value == null && getAddValue().stopWhenNull()); // связано с addValue, которое в свою очередь связано с intersect - в котором важно выйти быстро, если уже не подходит

            int hash = MapFact.colHash(key.hashCode());

            int i = hash & (table.length - 1);
            while (table[i] != null) {
                if (BaseUtils.hashEquals(table[i], key)) {
                    AddValue<K, V> addValue = getAddValue();
                    V addedValue = addValue.addValue(key, (V) vtable[i], value);
                    if (addValue.stopWhenNull() && addedValue == null)
                        return false;
                    vtable[i] = addedValue;
                    return true;
                }
                i = (i == table.length - 1 ? 0 : i + 1);
            }
            table[i] = key;
            vtable[i] = value;
            indexes[size++] = i;
            switchToStoredIfNeeded(size-1, size);
            return true;
        } else {
            return stored().add(key, value);
        }
    }

    @Override
    public V getObject(Object key) {
        if (!isStored()) {
            for (int i = MapFact.colHash(key.hashCode()) & (table.length - 1); table[i] != null; i = (i == table.length - 1 ? 0 : i + 1))
                if (BaseUtils.hashEquals(table[i], key))
                    return (V) vtable[i];
            return null;
        } else {
            return stored().getObject(key);
        }
    }

    public ImMap<K, V> immutable() {
        ImMap<K, V> simple = simpleImmutable();
        if(simple!=null)
            return simple;

        if (needToBeStored(this) && switchToStored(size, table, vtable, indexes)) {
            return stored().immutable();
        }

        if (!isStored()) {
            if (size < SetFact.useArrayMax) {
                Object[] keys = new Object[size];
                Object[] values = new Object[size];
                for (int i = 0; i < size; i++) {
                    keys[i] = getKey(i);
                    values[i] = getValue(i);
                }
                return new ArMap<>(size, keys, values);
            }
            if (size >= SetFact.useIndexedArrayMin) {
                Object[] keys = new Object[size];
                Object[] values = new Object[size];
                for (int i = 0; i < size; i++) {
                    keys[i] = getKey(i);
                    values[i] = getValue(i);
                }
                ArSet.sortArray(size, keys, values);
                return new ArIndexedMap<>(size, keys, values);
            }

            if (indexes.length > size * SetFact.factorNotResize) {
                int[] newIndexes = new int[size];
                System.arraycopy(indexes, 0, newIndexes, 0, size);
                indexes = newIndexes;
            }
        }
        return this;
    }

    protected MExclMap<K, V> copy() {
        return new HMap<>(this, true);
    }

    public void mapValue(int i, V value) {
        assert i<size;
        if (!isStored()) {
            vtable[indexes[i]] = value;
        } else {
            stored().mapValue(i, value);
        }
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return new HMap<>(this);
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new HMap<>(this);
    }

    @Override
    public ImOrderMap<K, V> toOrderMap() {
        return new HOrderMap<>(this);
    }

    @Override
    public ImSet<K> keys() {
        if (!isStored()) {
            return new HSet<>(size, table, indexes);
        } else {
            return stored().keys();
        }
    }

    public Object[] getTable() {
        if (!isStored()) {
            return table;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public Object[] getVTable() {
        if (!isStored()) {
            return vtable;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public void setIndexes(int[] indexes) {
        if (!isStored()) {
            this.indexes = indexes;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public int[] getIndexes() {
        if (!isStored()) {
            return indexes;
        } else {
            return stored().getIndexes();
        }
    }

    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        HMap<?, ?> map = (HMap<?, ?>) o;
        serializer.serialize(map.size, outStream);
        ArCol.serializeArray(map.table, serializer, outStream);
        ArCol.serializeArray(map.vtable, serializer, outStream);
        ArCol.serializeIntArray(map.indexes, serializer, outStream);
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        int size = (int) serializer.deserialize(inStream);
        Object[] table = ArCol.deserializeArray(inStream, serializer);
        Object[] vtable = ArCol.deserializeArray(inStream, serializer);
        int[] indexes = ArCol.deserializeIntArray(inStream, serializer);
        return new HMap<>(size, table, vtable, indexes);
    }

    public boolean isStored() {
        return size == STORED_FLAG;
    }

    public StoredHMap<K, V> stored() {
        return (StoredHMap<K, V>) table[0];
    }

    private void switchToStoredIfNeeded(int oldSize, int newSize) {
        if (oldSize <= LIMIT && newSize > LIMIT && needToBeStored(this)) {
            switchToStored(size, table, vtable, indexes);
        }
    }

    private static boolean needToBeStored(HMap<?, ?> map) {
        return !map.isStored() && map.size() > LIMIT && canBeStored(map);
    }

    private static boolean canBeStored(HMap<?, ?> map) {
        return isStoredArraysEnabled()
                && StoredArraySerializer.getInstance().canBeSerialized(map.getKey(0))
                && StoredArraySerializer.getInstance().canBeSerialized(map.getValue(0));
    }

    private boolean switchToStored(int size, Object[] keys, Object[] values, int[] indexes) {
        try {
            AddValue<K, V> addValue = (data instanceof AddValue ? (AddValue<K, V>) data : null);
            StoredHMap<K, V> storedMap =
                    new StoredHMap<>(size, (K[]) keys, (V[]) values, indexes, StoredArraySerializer.getInstance(), addValue);
            switchToStored(storedMap);
            return true;
        } catch (StoredArray.StoredArrayCreationException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void switchToStored(StoredHMap<K, V> storedMap) {
        this.table = new Object[]{storedMap};
        this.size = STORED_FLAG;
    }
}
