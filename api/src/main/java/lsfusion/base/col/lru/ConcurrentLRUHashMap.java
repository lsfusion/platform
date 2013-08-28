package lsfusion.base.col.lru;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: DAle
 * Date: 30.07.13
 * Time: 16:52
 */

/** 
 * Сверху построен по принципу {@link java.util.concurrent.ConcurrentHashMap}: разделен на сегменты (класс Segment), 
 * у каждого из которых есть свой Lock для синхронизации. Количество сегментов определяется параметром concurrencyLevel.
 * <p> Хранит текущее время, которое обновляется каждые UPDATE_CURTIME_FREQUENCY операций со структурой (put/get/containsKey)
 * <p> Ключи и значения структуры не могут быть равными null. Для имитации null-значений можно использовать ConcurrentLRUHashMap.Value.NULL  
 * <p> Внутренний класс Segment хранит реализует LRU-кэш, объединяя все Entry в двусвязный список, и переставляя элементы, 
 * к которым происходит обращение, в начало списка, запоминая время обращения. Если со время последнего обращения к элементу хэша
 * прошло более LRU_TIME_LIMIT миллисекунд, то элемент удаляется из стуктуры.
 * <p> Класс Segment блокирует внутреннюю структуру на операции put, но не блокирует в большинстве операций get/containsKey.
 * Достигается это хранением специального буфера, в который записываются чтения элементов. Запись в этот буфер происходит 
 * без блокирования внутренней структуры, что может привести к частичной потере информации о чтении. Сейчас информация из этого буфера 
 * переносится в LRU-кэш при переполнении этого буфера. Это означает, что информация о чтении может долгое время не попадать в LRU,
 * либо попадать с сильно отличающимся от исходного временем. Плюсом такого подхода является минимизация блокирующих операций.
 */
public class ConcurrentLRUHashMap<K, V> {
    public enum Value {NULL}    
    
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     */
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The default concurrency level for this table, used when not
     * otherwise specified in a constructor.
     */
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    private static final long LRU_TIME_LIMIT = 20 * 1000;

    /**
     * Mask value for indexing into segments. The upper bits of a
     * key's hash code are used to choose the segment.
     */
    private final int segmentMask;

    /**
     * Shift value for indexing within segments.
     */
    private final int segmentShift;

    /**
     * The segments, each of which is a specialized hash table
     */
    private final Segment[] segments;

    private long currentTime;

    private long operations = 0;

    private static final int UPDATE_CURTIME_FREQUENCY = 1 << 7;   
    private static final int UPDATE_CURTIME_FREQUENCY_MASK = UPDATE_CURTIME_FREQUENCY - 1;

    public ConcurrentLRUHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);

        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        // Find power-of-two sizes best matching arguments
        int sshift = 0;
        int ssize = 1;
        while (ssize < concurrencyLevel) {
            ++sshift;
            ssize <<= 1;
        }
        segmentShift = 32 - sshift;
        segmentMask = ssize - 1;
        this.segments = new Segment[ssize];

        int c = (initialCapacity + ssize - 1) / ssize;
        int cap = nearestPowerOf2(c);

        for (int i = 0; i < this.segments.length; ++i)
            this.segments[i] = new Segment<K,V>(cap, loadFactor);

        currentTime = System.currentTimeMillis();
    }

    public ConcurrentLRUHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
    }

    private void recordOperation() {
        ++operations;
        if ((operations & UPDATE_CURTIME_FREQUENCY_MASK) == 0) {
            currentTime = System.currentTimeMillis();
        }
    }

    public V get(Object key) {
        recordOperation();
        int hash = hash(key.hashCode());
        return segmentFor(hash).get(key, hash);
    }

    public V put(K key, V value) {
        recordOperation();
        int hash = hash(key.hashCode());
        return segmentFor(hash).put(key, hash, value);
    }

    public boolean containsKey(Object key) {
        recordOperation();
        int hash = hash(key.hashCode());
        return segmentFor(hash).containsKey(key, hash);
    }

    public int getSize() {
        int s = 0;
        for (int i = 0; i < segments.length; ++i) {
            s += segments[i].size;
        }
        return s;
    }

    /**
     * Returns the segment that should be used for key with given hash
     * @param hash the hash code for the key
     * @return the segment
     */
    final Segment<K,V> segmentFor(int hash) {
        return segments[(hash >>> segmentShift) & segmentMask];
    }

    private static int hash(int h) {
        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
        h += (h <<  15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h <<   3);
        h ^= (h >>>  6);
        h += (h <<   2) + (h << 14);
        return h ^ (h >>> 16);
    }

    final class Segment<K,V> {
        static final int BUFFER_SIZE = 32;
        static final int BUFFER_MASK = BUFFER_SIZE - 1;

        private Entry<K,V> head;
        private Entry<K,V> tail;

        private int threshold;
        private final float loadFactor;

        private int size; 
        private Entry[] table;

        private final Entry<K,V>[] buffer;
        private final AtomicLong bufferPos = new AtomicLong(0);
        private long bufferStart = 0;

        private final ReentrantLock changeLock = new ReentrantLock();

        public Segment(int initialCapacity, float loadFactor) {
            int capacity = nearestPowerOf2(initialCapacity);

            this.loadFactor = loadFactor;
            threshold = (int)(capacity * loadFactor);
            table = new Entry[capacity];

            buffer = new Entry[BUFFER_SIZE];

            tail = new Entry<K,V>(-1, null, null, null, 0);
            tail.before = tail.after = tail;
            head = tail;
        }

        public final V get(Object key, int hash) {
            Entry<K,V> e = getEntry(key, hash);
            if (e == null)
                return null;
            recordAccess(e);
            updateLRU();
            return e.value;
        }

        public final boolean containsKey(Object key, int hash) {
            Entry<K,V> e = getEntry(key, hash);
            if (e == null)
                return false;
            recordAccess(e);
            updateLRU();
            return true;
        }

        private void recordAccess(Entry<K,V> e) {
            final long curPos = bufferPos.get();
            buffer[((int) (curPos & BUFFER_MASK))] = e;
            bufferPos.set(curPos + 1);
            drainBufferIfNeeded(curPos + 1);
        }

        private boolean needToDrainBuffer(long curBufferPos) {
            return curBufferPos >= bufferStart + BUFFER_SIZE;            
        }
        
        private void drainBufferIfNeeded(long curPos) {
            if (needToDrainBuffer(curPos) && changeLock.tryLock()) {
                try {
                    final long end = Math.min(bufferPos.get(), bufferStart + BUFFER_SIZE);
                    for (long pos = bufferStart; pos < end; ++pos) {
                        final int bufPos = (int) (pos & BUFFER_MASK);
                        MoveToLRUHead(buffer[bufPos]);
                    }
                    bufferStart = end;
                } finally {
                    changeLock.unlock();
                }
            }
        }

        private void MoveToLRUHead(final Entry<K,V> e) {
            if (e != head) {
                e.removeFromLRU();
                e.addBeforeLRU(head);
            }
            e.setTime(currentTime);
            head = e;
        }

        private void updateLRU() {
            if (head != tail && currentTime - tail.before.getTime() > LRU_TIME_LIMIT) {
                try {
                    changeLock.lock();
                    Entry<K,V> last = tail.before;
                    while (head != tail && currentTime - last.getTime() > LRU_TIME_LIMIT) {
                        removeHashEntry(last);
                        last.removeFromLRU();
                        last = last.before;
                        if (last == tail) {
                            head = tail;
                        }
                    }
                } finally {
                    changeLock.unlock();
                }
            }
        }

        private int indexFor(int h, int length) {
            return h & (length-1);
        }

        private Entry<K,V> getEntry(Object key, int hash) {
            final Entry[] t = table;
            for (Entry<K,V> e = t[indexFor(hash, t.length)]; e != null; e = e.next) {
                if (e.hash == hash && (e.key == key || e.key.equals(key)))
                    return e;
            }
            return null;
        }

        public V put(K key, int hash, V value) {
            assert key != null;
            assert value != null;
            try {
                changeLock.lock();
                int i = indexFor(hash, table.length);
                for (Entry<K,V> e = table[i]; e != null; e = e.next) {
                    if (e.hash == hash && (e.key == key || e.key.equals(key))) {
                        V oldValue = e.value;
                        e.value = value;
                        recordAccess(e);
                        return oldValue;
                    }
                }
                addEntry(hash, key, value, i);
            } finally {
                updateLRU();
                changeLock.unlock();
            }
            return null;
        }

        private void addEntry(int hash, K key, V value, int bucketIndex) {
            createEntry(hash, key, value, bucketIndex);
            ++size;
            if (size >= threshold)
                resize(2 * table.length);
        }

        private void removeHashEntry(Entry<K,V> re) {
            int i = indexFor(re.hash, table.length);
            Entry<K,V> prev = table[i];
            Entry<K,V> e = prev;

            while (e != null) {
                Entry<K,V> next = e.next;
                if (e == re) {
                    size--;
                    if (prev == e)
                        table[i] = next;
                    else
                        prev.next = next;

                    return;
                }
                prev = e;
                e = next;
            }
        }

        private void resize(int newCapacity) {
            Entry[] oldTable = table;
            int oldCapacity = oldTable.length;
            if (oldCapacity == MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return;
            }

            Entry[] newTable = new Entry[newCapacity];
            transfer(newTable);
            table = newTable;
            threshold = (int)(newCapacity * loadFactor);
        }

        private void transfer(Entry[] newTable) {
            Entry[] src = table;
            int newCapacity = newTable.length;
            for (int j = 0; j < src.length; j++) {
                Entry<K,V> e = src[j];
                if (e != null) {
                    src[j] = null;
                    do {
                        Entry<K,V> next = e.next;
                        int i = indexFor(e.hash, newCapacity);
                        e.next = newTable[i];
                        newTable[i] = e;
                        e = next;
                    } while (e != null);
                }
            }
        }

        private void createEntry(int hash, K key, V value, int bucketIndex) {
            Entry<K,V> old = table[bucketIndex];
            Entry<K,V> e = new Entry<K,V>(hash, key, value, old, currentTime);
            table[bucketIndex] = e;
            e.addBeforeLRU(head);
            head = e;
        }
    }


    static final class Entry<K,V> implements Map.Entry<K,V> {
        private final K key;
        private V value;
        private final int hash;

        private Entry<K,V> next;
        private Entry<K,V> before, after;

        private long time;

        public Entry(int h, K k, V v, Entry<K,V> n, long t) {
            value = v;
            next = n;
            key = k;
            hash = h;
            time = t;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long t) {
            time = t;
        }

        public V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            ConcurrentLRUHashMap.Entry e = (ConcurrentLRUHashMap.Entry) o;
            return (getKey() == e.getKey() || getKey().equals(e.getKey())) && getValue() == e.getValue();
        }

        public int hashCode() {
            return (key==null ? 0 : key.hashCode()) ^ (value==null ? 0 : value.hashCode());
        }

        public String toString() {
            return getKey() + "=" + getValue();
        }

        private void removeFromLRU() {
            before.after = after;
            after.before = before;
        }

        private void addBeforeLRU(Entry<K,V> existingEntry) {
            after  = existingEntry;
            before = existingEntry.before;
            before.after = this;
            after.before = this;
        }
    }

    private static int nearestPowerOf2(int num) {
        int res = 1;
        while (res < num) {
            res <<= 1;
        }
        return res;
    }
}

