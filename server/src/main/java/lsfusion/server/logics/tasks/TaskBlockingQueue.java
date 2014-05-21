package lsfusion.server.logics.tasks;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TaskBlockingQueue<E extends Task.PriorityRunnable> extends AbstractQueue<E>
        implements BlockingQueue<E> {

    private final PriorityQueue<E> qDiameter;
    private final PriorityQueue<E> qLeastEstimated;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition notEmpty = lock.newCondition();
    private TreeSet<E> polled = new TreeSet<E>();

    public TaskBlockingQueue() {
        qDiameter = new PriorityQueue<E>();
        qLeastEstimated = new PriorityQueue<E>(11, Task.leastEstimated);
    }

    public boolean add(E e) {
        return offer(e);
    }

    /**
     * Inserts the specified element into this priority queue.
     * @param e the element to add
     * @return <tt>true</tt> (as specified by {@link java.util.Queue#offer})
     * @throws ClassCastException   if the specified element cannot be compared
     *                              with elements currently in the priority queue according to the
     *                              priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            boolean ok = qDiameter.add(e);
            assert ok;
            ok = qLeastEstimated.add(e);
            assert ok;
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void put(E e) {
        offer(e); // never need to block
    }

    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e); // never need to block
    }

    public void ensurePolled(E task) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            polled.add(task);
        } finally {
            lock.unlock();
        }
    }

    public void removePolled(E task) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            boolean removed = polled.remove(task);
            assert removed;
        } finally {
            lock.unlock();
        }
    }

    private E internalPoll() {
        Iterator<E> eIterator = polled.descendingIterator();
        E maxRunning = eIterator.hasNext() ? eIterator.next() : null;
        E maxDiamWaiting = qDiameter.peek();
        if (maxDiamWaiting == null) {
            assert qLeastEstimated.peek() == null;
            return null;
        }
        E leastWaiting = qLeastEstimated.peek();

        boolean removed;
        E polledTask;
        if (useLeastEstimated(maxRunning, maxDiamWaiting, leastWaiting)) {
            E leastPoll = qLeastEstimated.poll();
            removed = qDiameter.remove(leastPoll);
            polledTask = leastPoll;
        } else {
            E diamPoll = qDiameter.poll();
            removed = qLeastEstimated.remove(diamPoll);
            polledTask = diamPoll;
        }

        polled.add(polledTask);
        assert removed;
        return polledTask;
    }

    private E internalPeek() {
        E maxRunning = polled.size() > 0 ? polled.descendingIterator().next() : null;
        E maxDiamWaiting = qDiameter.peek();
        E leastWaiting = qLeastEstimated.peek();
        if (useLeastEstimated(maxRunning, maxDiamWaiting, leastWaiting)) {
            return leastWaiting;
        } else {
            return maxDiamWaiting;
        }
    }

    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return internalPoll();
        } finally {
            lock.unlock();
        }
    }

    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            try {
                while (qDiameter.size() == 0) {
                    notEmpty.await();
                }
            } catch (InterruptedException ie) {
                notEmpty.signal(); // propagate to non-interrupted thread
                throw ie;
            }
            E x = internalPoll();
            assert x != null;
            return x;
        } finally {
            lock.unlock();
        }
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (; ; ) {
                E x = internalPoll();
                if (x != null) {
                    return x;
                }
                if (nanos <= 0) {
                    return null;
                }
                try {
                    nanos = notEmpty.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    notEmpty.signal(); // propagate to non-interrupted thread
                    throw ie;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return internalPeek();
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int size = qDiameter.size();
            assert size == qLeastEstimated.size();
            return size;
        } finally {
            lock.unlock();
        }
    }

    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    public boolean remove(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            boolean removed = qDiameter.remove(o);
            boolean lRemoved = qLeastEstimated.remove(o);
            assert removed == lRemoved;
            return removed;
        } finally {
            lock.unlock();
        }
    }

    public boolean contains(Object o) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            boolean contains = qDiameter.contains(o);
            boolean lContains = qLeastEstimated.contains(o);
            assert contains == lContains;
            return contains;
        } finally {
            lock.unlock();
        }
    }

    public Object[] toArray() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return qDiameter.toArray();
        } finally {
            lock.unlock();
        }
    }

    public String toString() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return qDiameter.toString();
        } finally {
            lock.unlock();
        }
    }

    public int drainTo(Collection<? super E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = 0;
            E e;
            while ((e = internalPoll()) != null) {
                c.add(e);
                ++n;
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        if (maxElements <= 0) {
            return 0;
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = 0;
            E e;
            while (n < maxElements && (e = internalPoll()) != null) {
                c.add(e);
                ++n;
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            qDiameter.clear();
            qLeastEstimated.clear();
        } finally {
            lock.unlock();
        }
    }

    public <T> T[] toArray(T[] a) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return qDiameter.toArray(a);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns an iterator over the elements in this queue. The
     * iterator does not return the elements in any particular order.
     * The returned <tt>Iterator</tt> is a "weakly consistent"
     * iterator that will never throw {@link
     * java.util.ConcurrentModificationException}, and guarantees to traverse
     * elements as they existed upon construction of the iterator, and
     * may (but is not guaranteed to) reflect any modifications
     * subsequent to construction.
     * @return an iterator over the elements in this queue
     */
    public Iterator<E> iterator() {
        return new Itr(toArray());
    }

    private static <E extends Task.PriorityRunnable> boolean useLeastEstimated(E maxRunning, E maxDiamWaiting, E leastWaiting) {
        return maxRunning != null && maxRunning.getComplexity() > maxDiamWaiting.getComplexity(); //- maxRunning.getBaseComplexity() - leastWaiting.getBaseComplexity() 
    }

    /**
     * Snapshot iterator that works off copy of underlying q array.
     */
    private class Itr implements Iterator<E> {
        final Object[] array; // Array of all elements
        int cursor;           // index of next element to return;
        int lastRet;          // index of last element, or -1 if no such

        Itr(Object[] array) {
            lastRet = -1;
            this.array = array;
        }

        public boolean hasNext() {
            return cursor < array.length;
        }

        public E next() {
            if (cursor >= array.length) {
                throw new NoSuchElementException();
            }
            lastRet = cursor;
            return (E) array[cursor++];
        }

        public void remove() {
            if (lastRet < 0) {
                throw new IllegalStateException();
            }
            Object x = array[lastRet];
            lastRet = -1;
            // Traverse underlying queue to find == element,
            // not just a .equals element.
            lock.lock();
            try {
                for (Iterator it = qDiameter.iterator(); it.hasNext(); ) {
                    if (it.next() == x) {
                        it.remove();
                        break;
                    }
                }
                for (Iterator it = qLeastEstimated.iterator(); it.hasNext(); ) {
                    if (it.next() == x) {
                        it.remove();
                        break;
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
