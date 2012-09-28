package platform.base;

import java.util.*;

// дублируем QuickSet
public abstract class QuickMap<K, V> {
    public int size;
    protected Object[] table;
    protected int[] htable;
    protected Object[] vtable;

    protected int[] indexes; // номера в таблице

    protected abstract V addValue(K key, V prevValue, V newValue);

    protected abstract boolean containsAll(V who, V what);

    private final float loadFactor;

    public QuickMap() {
        loadFactor = 0.3f;

        table = new Object[8];
        htable = new int[table.length];
        vtable = new Object[8];

        indexes = new int[(int) (table.length * loadFactor)];
    }

    public QuickMap(QuickMap<? extends K, ? extends V> set) {
        size = set.size;
        loadFactor = set.loadFactor;

        table = set.table.clone();
        htable = set.htable.clone();
        vtable = set.vtable.clone();

        indexes = set.indexes.clone();
    }

    protected QuickMap(K key, V value) {
        this();
        add(key, value);
    }

    protected QuickMap(Iterable<? extends K> keys, V value) {
        this();
        addAll(keys, value);
    }

    public K getKey(int i) {
        return (K) table[indexes[i]];
    }

    public V getValue(int i) {
        return (V) vtable[indexes[i]];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    private void resize(int length) {
        int[] newIndexes = new int[(int) (length * loadFactor)];

        Object[] newTable = new Object[length];
        int[] newHTable = new int[length];
        Object[] newVTable = new Object[length];
        for (int i = 0; i < size; i++) {
            int newHash = (htable[indexes[i]] & (length - 1));
            while (newTable[newHash] != null) newHash = (newHash == length - 1 ? 0 : newHash + 1);

            newTable[newHash] = table[indexes[i]];
            newHTable[newHash] = htable[indexes[i]];
            newVTable[newHash] = vtable[indexes[i]];

            newIndexes[i] = newHash;
        }

        table = newTable;
        htable = newHTable;
        vtable = newVTable;

        indexes = newIndexes;
    }

    public static int hash(int h) { // копися с hashSet'а
        h ^= (h >>> 20) ^ (h >>> 12);
        return (h ^ (h >>> 7) ^ (h >>> 4));
    }

    public boolean add(K key, V value) {
        return add(key, value, true);
    }

    private boolean add(int index, QuickMap<? extends K, ? extends V> map) {
        return add(map.table[map.indexes[index]], map.htable[map.indexes[index]], map.vtable[map.indexes[index]], true);
    }

    public void set(K key, V value) {
        add(key, value, false);
    }

    private boolean add(K key, V value, boolean add) {
        return add(key, hash(key.hashCode()), value, true);
    }

    private boolean add(Object key, int hash, Object value, boolean add) {
        int i = hash & (table.length - 1);
        while (table[i] != null) {
            if (htable[i] == hash && table[i].equals(key)) {
                if (add) {
                    value = addValue((K) key, (V) vtable[i], (V) value);
                    if (value == null)
                        return false;
                }
                vtable[i] = value;
                return true;
            }
            i = (i == table.length - 1 ? 0 : i + 1);
        }
        table[i] = key;
        htable[i] = hash;
        vtable[i] = value;
        indexes[size++] = i;
        if (size >= indexes.length) resize(2 * table.length);
        return true;
    }

    public boolean addAll(QuickMap<? extends K, ? extends V> set) {
        for (int i = 0; i < set.size; i++)
            if (!add(i, set))
                return false;
        return true;
    }

    public boolean addAll(QuickMap<? extends K, ? extends V> set, QuickSet<K> skip) {
        for (int i = 0; i < set.size; i++)
            if (!skip.contains(set.getKey(i)) &&  !add(i, set))
                return false;
        return true;
    }

    public void addAll(Iterable<? extends K> keys, V value) {
        for(K key : keys)
            add(key, value);
    }

    public boolean containsAll(QuickMap<K, V> set) {
        if (size > set.size) return false; // если больше то содержать не может

        for (int i = 0; i < size; i++) {
            V inSet = set.get(getKey(i), htable[indexes[i]]);
            if (inSet == null || !(containsAll(getValue(i), inSet))) return false;
        }
        return true;
    }

    // в некоторых случаях из-за багов Java на instanceof не проверишь
    private V getObject(Object key, int hash) {
        for (int i = hash & (table.length - 1); table[i] != null; i = (i == table.length - 1 ? 0 : i + 1))
            if (htable[i] == hash && table[i].equals(key))
                return (V) vtable[i];
        return null;
    }

    private V get(K key, int hash) {
        return getObject(key, hash);
    }

    public V getObject(Object key) {
        return getObject(key, hash(key.hashCode()));
    }

    public V get(K key) {
        return getObject(key);
    }

    public V getPartial(K key) { // временно
        return getObject(key);
    }

    @Override
    public String toString() {
        String result = "";
        for (int i = 0; i < size; i++)
            result = (result.length() == 0 ? "" : result + ",") + table[indexes[i]] + " - " + vtable[indexes[i]];
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (getClass() != obj.getClass()) return false;

        QuickMap map = (QuickMap) obj;
        if (map.size != size) return false;

        for (int i = 0; i < size; i++) {
            Object mapValue = map.get(table[indexes[i]], htable[indexes[i]]);
            if (mapValue == null || !mapValue.equals(vtable[indexes[i]])) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (int i = 0; i < size; i++)
            hash += htable[indexes[i]] ^ vtable[indexes[i]].hashCode();
        return hash;
    }

    public Collection<K> keys() {
        Collection<K> keys = new ArrayList<K>();
        for (int i = 0; i < size; i++)
            keys.add(getKey(i));
        return keys;
    }

    public Set<K> keySet() {
        Set<K> keys = new HashSet<K>();
        for (int i = 0; i < size; i++)
            keys.add(getKey(i));
        return keys;
    }

    public QuickSet<K> keyQuickSet() {
        QuickSet<K> keys = new QuickSet<K>();
        for (int i = 0; i < size; i++)
            keys.add(getKey(i));
        return keys;
    }

    public Iterable<K> keyIt() {
        return new Iterable<K>() {
            public Iterator<K> iterator() {
                return new Iterator<K>() {
                    int i=0;
                    public boolean hasNext() {
                        return i<size;
                    }

                    public K next() {
                        return getKey(i++);
                    }

                    public void remove() {
                        throw new RuntimeException("not supported");
                    }
                };
            }
        };
    }

    public Iterable<V> valueIt() {
        return new Iterable<V>() {
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    int i=0;
                    public boolean hasNext() {
                        return i<size;
                    }

                    public V next() {
                        return getValue(i++);
                    }

                    public void remove() {
                        throw new RuntimeException("not supported");
                    }
                };
            }
        };
    }

    public K getSingleKey() {
        assert size == 1;
        return getKey(0);
    }

    public boolean containsNullValue() {
        for (int i = 0; i < size; i++) {
            if (vtable[indexes[i]] == null) {
                return true;
            }
        }
        return false;
    }

    public <EK extends K> QuickMap<EK, V> filterKeys(QuickSet<? extends EK> keys) {
        QuickMap<EK, V> result = new SimpleMap<EK, V>();
        for (int i=0;i<keys.size;i++) {
            EK key = keys.get(i);
            V value = get(key);
            if(value!=null)
                result.add(key, value);
        }
        return result;
    }

    public <EK extends K> QuickMap<EK, V> filterInclKeys(QuickSet<? extends EK> keys) {
        QuickMap<EK, V> result = new SimpleMap<EK, V>();
        for (int i=0;i<keys.size;i++) {
            EK key = keys.get(i);
            V value = get(key);
            assert value!=null;
            result.add(key, value);
        }
        return result;
    }
}
