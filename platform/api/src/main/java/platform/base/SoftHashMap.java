package platform.base;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * SoftHashMap
 */

public class SoftHashMap<K,V> {

    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private Entry[] table;
    private int size;

    private int threshold;

    private final float loadFactor;

    private final ReferenceQueue<K> queue = new ReferenceQueue<K>();

    private volatile int modCount;

    public SoftHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Initial Capacity: "+
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal Load factor: "+
                                               loadFactor);
        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;
        table = new Entry[capacity];
        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
    }

    public SoftHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public SoftHashMap() {
        loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = DEFAULT_INITIAL_CAPACITY;
        table = new Entry[DEFAULT_INITIAL_CAPACITY];
    }

    // internal utilities

    static int indexFor(int h, int length) {
        return h & (length-1);
    }

    private void expungeStaleEntries() {
	Entry<K,V> e;
        while ( (e = (Entry<K,V>) queue.poll()) != null) {
            int h = e.hash;
            int i = indexFor(h, table.length);

            Entry<K,V> prev = table[i];
            Entry<K,V> p = prev;
            while (p != null) {
                Entry<K,V> next = p.next;
                if (p == e) {
                    if (prev == e)
                        table[i] = next;
                    else
                        prev.next = next;
                    e.next = null;  // Help GC
                    e.value = null; //  "   "
                    size--;
                    break;
                }
                prev = p;
                p = next;
            }
        }
    }

    private Entry[] getTable() {
        expungeStaleEntries();
        return table;
    }

    public int size() {
        if (size == 0)
            return 0;
        expungeStaleEntries();
        return size;
    }

    static int hash(int h) {
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    public V get(Object key) {
        int h = hash(key.hashCode());
        Entry[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry<K,V> e = tab[index];
        while (e != null) {
            if (e.hash == h && key.equals(e.get()))
                return e.value;
            e = e.next;
        }
        return null;
    }

    public V remove(Object key) {
        int h = hash(key.hashCode());
        Entry[] tab = getTable();
        int i = indexFor(h, tab.length);
        Entry<K,V> prev = tab[i];
        Entry<K,V> e = prev;

        while (e != null) {
            Entry<K,V> next = e.next;
            if (h == e.hash && key.equals(e.get())) {
                modCount++;
                size--;
                if (prev == e)
                    tab[i] = next;
                else
                    prev.next = next;
                return e.value;
            }
            prev = e;
            e = next;
        }

        return null;
    }

    public V put(K k, V value) {
        int h = hash(k.hashCode());
        Entry[] tab = getTable();
        int i = indexFor(h, tab.length);

        for (Entry<K,V> e = tab[i]; e != null; e = e.next) {
            if (h == e.hash && k.equals(e.get())) {
                V oldValue = e.value;
                if (value != oldValue)
                    e.value = value;
                return oldValue;
            }
        }

        modCount++;
    	Entry<K,V> e = tab[i];
        tab[i] = new Entry<K,V>(k, value, queue, h, e);
        if (++size >= threshold)
            resize(tab.length * 2);
        return null;
    }

    void resize(int newCapacity) {
        Entry[] oldTable = getTable();
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        Entry[] newTable = new Entry[newCapacity];
        transfer(oldTable, newTable);
        table = newTable;

        if (size >= threshold / 2) {
            threshold = (int)(newCapacity * loadFactor);
        } else {
            expungeStaleEntries();
            transfer(newTable, oldTable);
            table = oldTable;
        }
    }

    private void transfer(Entry[] src, Entry[] dest) {
        for (int j = 0; j < src.length; ++j) {
            Entry<K,V> e = src[j];
            src[j] = null;
            while (e != null) {
                Entry<K,V> next = e.next;
                Object key = e.get();
                if (key == null) {
                    e.next = null;  // Help GC
                    e.value = null; //  "   "
                    size--;
                } else {
                    int i = indexFor(e.hash, dest.length);
                    e.next = dest[i];
                    dest[i] = e;
                }
                e = next;
            }
        }
    }

    private static class Entry<K,V> extends SoftReference<K> {
        private V value;
        private final int hash;
        private Entry<K,V> next;

        Entry(K key, V value,
	      ReferenceQueue<K> queue,
              int hash, Entry<K,V> next) {
            super(key, queue);
            this.value = value;
            this.hash  = hash;
            this.next  = next;
        }
    }
}
