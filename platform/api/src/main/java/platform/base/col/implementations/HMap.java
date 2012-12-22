package platform.base.col.implementations;

import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.implementations.abs.AMRevMap;
import platform.base.col.implementations.order.HOrderMap;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.mutable.*;
import platform.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import platform.base.col.interfaces.mutable.mapvalue.ImValueMap;

// дублируем HSet
public class HMap<K, V> extends AMRevMap<K, V> {
    public int size;
    public Object[] table;
    protected Object[] vtable;

    public int[] indexes; // номера в таблице

    public int size() {
        return size;
    }

    private final static float loadFactor = 0.3f;

    public HMap(AddValue<K, V> addValue) {
        super(addValue);

        table = new Object[8];
        vtable = new Object[8];

        indexes = new int[(int) (table.length * loadFactor)];
    }

    public HMap(HMap<? extends K, ? extends V> map, boolean clone) {
        assert clone;
        
        size = map.size;

        table = map.table.clone();
        vtable = map.vtable.clone();

        indexes = map.indexes.clone();
    }

    public HMap(int size, Object[] table, Object[] vtable, int[] indexes) {
        this.size = size;
        this.table = table;
        this.vtable = vtable;
        this.indexes = indexes;
    }

    public HMap(HMap<? extends K, ? extends V> map, AddValue<K, V> addValue) {
        super(addValue);

        size = map.size;

        table = map.table.clone();
        vtable = map.vtable.clone();

        indexes = map.indexes.clone();
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

    // конструктор на valueMap
    private HMap(Object[] table, int[] indexes, int size) {
        this.table = table;
        this.indexes = indexes;

        this.size = size;
        vtable = new Object[table.length];
    }
    
    public HMap(HMap<? extends K, ?> map) {
        this(map.table, map.indexes, map.size);
    }

    public HMap(HSet<? extends K> set) {
        this(set.table, set.indexes, set.size);
    }

    public K getKey(int i) {
        assert i<size;
        return (K) table[indexes[i]];
    }

    public V getValue(int i) {
        assert i<size;
        return (V) vtable[indexes[i]];
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
        if (size >= indexes.length) resize(2 * table.length);
        assert !(value==null && getAddValue().stopWhenNull()); // связано с addValue, которое в свою очередь связано с intersect - в котором важно выйти быстро, если уже не подходит

        int hash = MapFact.colHash(key.hashCode());
        
        int i = hash & (table.length - 1);
        while (table[i] != null) {
            if (BaseUtils.hashEquals(table[i], key)) {
                AddValue<K, V> addValue = getAddValue();
                V addedValue = addValue.addValue(key, (V) vtable[i], value);
                if(addValue.stopWhenNull() && addedValue==null)
                    return false;
                vtable[i] = addedValue;
                return true;
            }
            i = (i == table.length - 1 ? 0 : i + 1);
        }
        table[i] = key;
        vtable[i] = value;
        indexes[size++] = i;
        return true;
    }

    @Override
    public V getObject(Object key) {
        for (int i = MapFact.colHash(key.hashCode()) & (table.length - 1); table[i] != null; i = (i == table.length - 1 ? 0 : i + 1))
            if (BaseUtils.hashEquals(table[i], key))
                return (V) vtable[i];
        return null;
    }

    public ImMap<K, V> immutable() {
        ImMap<K, V> simple = simpleImmutable();
        if(simple!=null)
            return simple;

        if(size < SetFact.useArrayMax) {
            Object[] keys = new Object[size];
            Object[] values = new Object[size];
            for(int i=0;i<size;i++) {
                keys[i] = getKey(i);
                values[i] = getValue(i);
            }
            return new ArMap<K, V>(size, keys, values);
        }
        if(size >= SetFact.useIndexedArrayMin) {
            Object[] keys = new Object[size];
            Object[] values = new Object[size];
            for(int i=0;i<size;i++) {
                keys[i] = getKey(i);
                values[i] = getValue(i);
            }
            ArSet.sortArray(size, keys, values);
            return new ArIndexedMap<K, V>(size, keys, values);
        }

        if(indexes.length > size * SetFact.factorNotResize) {
            int[] newIndexes = new int[size];
            System.arraycopy(indexes, 0, newIndexes, 0, size);
            indexes = newIndexes;
        }
        return this;
    }

    public ImMap<K, V> immutableCopy() {
        return new HMap<K, V>(this, true);
    }

    public void mapValue(int i, V value) {
        assert i<size;
        vtable[indexes[i]] = value;
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return new HMap<K, M>(this);
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new HMap<K, M>(this);
    }

    @Override
    public ImOrderMap<K, V> toOrderMap() {
        return new HOrderMap<K, V>(this);
    }

    @Override
    public HSet<K> keys() {
        return new HSet<K>(size, table, indexes);
    }
}
