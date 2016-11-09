package lsfusion.base.col.lru;

import lsfusion.base.BaseUtils;
import lsfusion.base.Processor;
import lsfusion.base.WeakIdentityHashSet;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.mutable.add.MAddCol;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static lsfusion.base.col.lru.LRUUtil.*;

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

public abstract class ALRUMap<E extends ALRUMap.AEntry<E>, S extends ALRUMap.ASegment> {

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
    private final S[] segments;

    protected int currentTime;
    private static long startTime = System.currentTimeMillis();
    private void updateCurrentTime() {
        currentTime = (int) ((System.currentTimeMillis() - startTime) >> 10);
    }

    private long operations = 0;

    protected abstract S[] createSegments(int size);
    protected abstract S createSegment(int cap, float loadFactor);
    
    static final WeakIdentityHashSet<ALRUMap> allMaps = new WeakIdentityHashSet<>(); 

    public ALRUMap(int initialCapacity, float loadFactor, int concurrencyLevel, Strategy expireStrategy) {
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
        this.segments = createSegments(ssize);

        int c = (initialCapacity + ssize - 1) / ssize;
        int cap = nearestPowerOf2(c);

        for (int i = 0; i < this.segments.length; ++i)
            this.segments[i] = createSegment(cap, loadFactor);

        updateCurrentTime();
        this.expireStrategy = expireStrategy;
        
        allMaps.add(this);
    }

    private final Strategy expireStrategy;
    
    protected ALRUMap(Strategy expireStrategy) {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, expireStrategy);
    }

    protected void recordOperation() {
        ++operations;
        if ((operations & UPDATE_CURTIME_FREQUENCY_MASK) == 0) {
            updateCurrentTime();
        }
    }
    
    public static void forceRemoveAllLRU(double percent) {
        for(ALRUMap map : allMaps)
            map.forceRemoveLRU(percent);
    }

    public static void forceRandomRemoveAllLRU(double percent) {
        for(ALRUMap map : allMaps)
            map.forceRemoveLRU(percent);
    }

    protected void forceRemoveLRU(double percent) {
        for(S seg : segments)
            seg.forceRemoveLRU(percent);
    }

    protected void forceRandomRemoveLRU(double percent) {
        for(S seg : segments)
            seg.forceRemoveLRU(percent);
    }

    protected void proceedSafeLockLRUEEntries(Processor<E> set) {
        for(S seg : segments)
            seg.proceedSafeLockLRUEntries(set);
    }

/*    public V get(K key) {
        recordOperation();
        int hash = hashKey(key);
        return segmentFor(hash).get(key, hash);
    }

    public V put(K key, V value) {
        recordOperation();
        int hash = hashKey(key);
        return segmentFor(hash).put(key, hash, value);
    }*/

    public int getSize() {
        int s = 0;
        for (S segment : segments) {
            s += segment.size;
        }
        return s;
    }

    /**
     * Returns the segment that should be used for key with given hash
     * @param hash the hash code for the key
     * @return the segment
     */
    final S segmentFor(int hash) {
        return segments[(hash >>> segmentShift) & segmentMask];
    }

    abstract class ASegment {
        static final int BUFFER_SIZE = 32;
        static final int BUFFER_MASK = BUFFER_SIZE - 1;

        private E head;
        private E tail;
        
        private E startTail; 

        private int threshold;
        private final float loadFactor;

        protected int size;
        protected E[] table;

        private final E[] buffer;
        private final AtomicLong bufferPos = new AtomicLong(0);
        private long bufferStart = 0;

        protected final ReentrantLock changeLock = new ReentrantLock();

        protected abstract E[] createEntries(int size);
        protected abstract E createTail();
            
        public ASegment(int initialCapacity, float loadFactor) {
            int capacity = nearestPowerOf2(initialCapacity);

            this.loadFactor = loadFactor;
            threshold = (int)(capacity * loadFactor);
            table = createEntries(capacity);

            buffer = createEntries(BUFFER_SIZE);

            startTail = createTail();
            startTail.setBefore(startTail);
            startTail.setAfter(startTail);
            
            tail = startTail; 
            head = startTail;
        }

        protected void recordAccess(E e) {
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
                        if (buffer[bufPos].isValid()) {
                            moveToLRUHead(buffer[bufPos]);
                        }
                    }
                    bufferStart = end;
                } finally {
                    changeLock.unlock();
                }
            }
        }
        
        private void moveToLRUHead(final E e) {
            if (e != head) {
                e.removeFromLRU();
                e.addBeforeLRU(head);
            }
            e.setTime(currentTime);
            head = e;
            assert tail.getAfter() == head && head.getBefore() == tail;
            assert head != head.getAfter() || head == tail;
        }

        protected void updateLRU() {
            double adjustedExpireTime = expireStrategy.getExpireTime();
            if (head != tail && currentTime - tail.getBefore().getTime() > adjustedExpireTime) {
                changeLock.lock();
                try {
                    E last = tail.getBefore();
                    while (head != tail && currentTime - last.getTime() > adjustedExpireTime) {
                        assert last != tail;
                        last = removeLRU(last);
                    }
                } finally {
                    changeLock.unlock();
                }
            }
        }

        protected void forceRemoveLRU(double percent) {
            changeLock.lock();
            try {
                double resultSize = size * (1.0 - percent);
                E last = tail.getBefore();
                while (head != tail && size > resultSize) {
                    assert last != tail;
                    last = removeLRU(last);
                }
            } finally {
                changeLock.unlock();
            }
        }

        protected void forceRandomRemoveLRU(double percent) {
            changeLock.lock();
            try {
                int kept = 0;
                int dropped = 0;
                E last = tail.getBefore();
                while (head != tail) {
                    assert last != tail;
                    if(dropped == 0 || ((double)kept / (double)(kept + dropped)) > percent) {
                        last = removeLRU(last);
                        dropped++;
                    } else
                        kept++;
                }
            } finally {
                changeLock.unlock();
            }
        }

        protected void proceedSafeLockLRUEntries(Processor<E> processor) {
            changeLock.lock();
            MAddCol<E> entries = ListFact.mAddCol(size);
            try {
                E entry = tail.getBefore();
                while (entry != head) {
                    assert entry != tail;
                    entries.add(entry);
                    entry = entry.getBefore();
                }
            } finally {
                changeLock.unlock();
            }
            for(int i=0,size=entries.size();i<size;i++) {
                processor.proceed(entries.get(i));
            }
        }

        protected E removeLRU(E last) {
            assert last.isValid();

            removeHashEntry(last);
            E prev = last.getBefore();
            last.removeFromLRU();
            if (prev == tail) {
                head = tail.getAfter();
            }
            return prev;
        }

        protected int indexFor(int h, int length) {
            return h & (length-1);
        }

/*        public final V get(K key, int hash) {
            final E[] t = table;
            for (E e = t[indexFor(hash, t.length)]; e != null; e = e.next) {
                if (BaseUtils.hashEquals(e.key, key)) {
                    recordAccess(e);
                    updateLRU();
                    return e.value;
                }
            }
            return null;
        }

        public V put(K key, int hash, V value) {
            assert key != null;
            assert value != null;
            changeLock.lock();
            try {
                int i = indexFor(hash, table.length);
                for (E e = table[i]; e != null; e = e.next) {
                    if (BaseUtils.hashEquals(e.key, key)) {
                        V oldValue = e.value;
                        e.value = value;
                        recordAccess(e);
                        return oldValue;
                    }
                }
                E e = new Entry<K,V>(key, value, table[i], currentTime);
                
                regEntry(e, i);
            } finally {
                changeLock.unlock();
                updateLRU();
            }
            return null;
        }
  */
        protected void regEntry(E entry, int bucketIndex) {
            table[bucketIndex] = entry;

            entry.addBeforeLRU(head);
            head = entry;
            assert tail.getAfter() == head && head.getBefore() == tail;
            assert head != head.getAfter() || head == tail;

            ++size;
            if (size >= threshold)
                resize(2 * table.length);
        }

        private void removeHashEntry(E re) {
            assert re != startTail;
            int i = indexFor(re.hashKey(), table.length);
            E prev = table[i];
            E e = prev;

            while (e != null) {
                E next = e.getNext();
                if (e == re) {
                    size--;
                    if (prev == e)
                        table[i] = next;
                    else
                        prev.setNext(next);
                    re.setNext(null);
                    return;
                }
                prev = e;
                e = next;
            }
            assert false;
        }

        private void resize(int newCapacity) {
            E[] oldTable = table;
            int oldCapacity = oldTable.length;
            if (oldCapacity == MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return;
            }

            E[] newTable = createEntries(newCapacity);
            transfer(newTable);
            table = newTable;
            threshold = (int)(newCapacity * loadFactor);
        }

        private void transfer(E[] newTable) {
            E[] src = table;
            int newCapacity = newTable.length;
            for (int j = 0; j < src.length; j++) {
                E e = src[j];
                if (e != null) {
                    src[j] = null;
                    do {
                        E next = e.getNext();
                        int i = indexFor(e.hashKey(), newCapacity);
                        e.setNext(newTable[i]);
                        newTable[i] = e;
                        e = next;
                    } while (e != null);
                }
            }
        }
    }


    interface AEntry<E extends AEntry<E>> {
        E getNext();

        void setNext(E next);

        E getBefore();

        void setBefore(E before);

        E getAfter();

        void setAfter(E after);

        int getTime();

        void setTime(int time);

        int hashKey();

        void removeFromLRU();
        
        boolean isValid(); // not removed

        void addBeforeLRU(E existingEntry);
    }
    
    protected static <T> boolean optEquals(T object1, T object2) {
        return object1 == object2 || object1.equals(object2);
    }

}
