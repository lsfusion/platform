package lsfusion.base.col.implementations.stored;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.implementations.abs.AMRevMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

public class StoredHMap<K, V> extends AMRevMap<K, V> {
    private int size;
    private int[] indexes; // номера в таблице

    private StoredArray<K> table;
    private StoredArray<V> vtable;

    private final static float loadFactor = 0.3f;

    public StoredHMap(StoredHMap<? extends K, ? extends V> map, AddValue<K, V> addValue) {
        super(addValue);
        size = map.size;
        indexes = map.indexes.clone();
        table = new StoredArray<>(map.table);
        vtable = new StoredArray<>(map.vtable);
    }

    public StoredHMap(StoredHMap<? extends K, ? extends V> map, boolean clone) {
        this(map, null);
        assert clone;
    }

    public StoredHMap(int size, K[] table, V[] vtable, int[] indexes, StoredArraySerializer serializer) throws StoredArray.StoredArrayCreationException {
        this(size, new StoredArray<>(table, serializer), new StoredArray<>(vtable, serializer), indexes);
    }

    private StoredHMap(int size, K[] table, int[] indexes, StoredArraySerializer serializer) throws StoredArray.StoredArrayCreationException {
        this(size, new StoredArray<>(table, serializer), new StoredArray<>(table.length, serializer), indexes);
    }

    private StoredHMap(int size, StoredArray<K> table, int[] indexes) {
        this(size, table, new StoredArray<>(table.size(), table.getSerializer()), indexes);
    }

    public StoredHMap(int size, StoredArray<K> table, StoredArray<V> vtable, int[] indexes) {
        this.size = size;
        this.indexes = indexes;
        this.table = table;
        this.vtable = vtable;
    }

    public StoredHMap(StoredHMap<K, ?> map) {
        this(map.size, map.table, map.indexes);
    }

    public StoredHMap(StoredHSet<K> set) {
        this(set.size(), set.getTable(), set.getIndexes());
    }

    public int size() {
        return size;
    }

    public K getKey(int i) {
        assert i<size;
        return table.get(indexes[i]);
    }

    public V getValue(int i) {
        assert i<size;
        return vtable.get(indexes[i]);
    }

    private void resize(int length) {
        int[] newIndexes = new int[(int) (length * loadFactor) + 1];

        StoredArray<K> newTable = new StoredArray<>(length, table.getSerializer());
        StoredArray<V> newVTable = new StoredArray<>(length, table.getSerializer());
        for (int i = 0; i < size; i++) {
            K key = table.get(indexes[i]);
            int newHash = MapFact.colHash(key.hashCode()) & (length - 1);
            while (newTable.get(newHash) != null) {
                newHash = (newHash == length-1 ? 0 : newHash+1);
            }

            newTable.set(newHash, key);
            newVTable.set(newHash, vtable.get(indexes[i]));
            newIndexes[i] = newHash;
        }

        table = newTable;
        vtable = newVTable;
        indexes = newIndexes;
    }

    public boolean add(K key, V value) {
        if (size >= indexes.length) resize(2 * table.size());
        assert !(value==null && getAddValue().stopWhenNull()); // связано с addValue, которое в свою очередь связано с intersect - в котором важно выйти быстро, если уже не подходит

        int hash = MapFact.colHash(key.hashCode());
        int i = hash & (table.size() - 1);
        while (table.get(i) != null) {
            if (BaseUtils.hashEquals(table.get(i), key)) {
                AddValue<K, V> addValue = getAddValue();
                V addedValue = addValue.addValue(key, vtable.get(i), value);
                if (addValue.stopWhenNull() && addedValue==null)
                    return false;
                vtable.set(i, addedValue);
                return true;
            }
            i = (i == table.size()-1 ? 0 : i+1);
        }
        table.set(i, key);
        vtable.set(i, value);
        indexes[size++] = i;
        return true;
    }

    @Override
    public V getObject(Object key) {
        int start = MapFact.colHash(key.hashCode()) & (table.size()-1);
        for (int i = start; table.get(i) != null; i = (i == table.size()-1 ? 0 : i+1))
            if (BaseUtils.hashEquals(table.get(i), key))
                return vtable.get(i);
        return null;
    }

    public ImMap<K, V> immutable() {
        return this;
    }

    protected MExclMap<K, V> copy() {
        return new StoredHMap<>(this, true);
    }

    public void mapValue(int i, V value) {
        assert i<size;
        vtable.set(indexes[i], value);
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return new StoredHMap<>(this);
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new StoredHMap<>(this);
    }

    @Override
    public ImOrderMap<K, V> toOrderMap() {
        // todo [dale]: uncomment
//        return new StoredHOrderMap<>(this);
        return null;
    }

    @Override
    public StoredHSet<K> keys() {
        return new StoredHSet<>(size, table, indexes);
    }
}
