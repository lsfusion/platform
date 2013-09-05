package lsfusion.base.col.lru;

import lsfusion.base.BaseUtils;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public abstract class ALRUWMap<W, E extends ALRUWMap.AEntry<W, E>, S extends ALRUWMap.ASegment> extends ALRUMap<E, S> {

    protected ALRUWMap(LRUUtil.Strategy expireStrategy) {
        super(expireStrategy);
    }

    private final static Object weakTail = new Object();
    protected W weakTail() {
        return (W) weakTail;
    } 
    
    
    abstract class ASegment extends ALRUMap<E, S>.ASegment {
        
        protected ReferenceQueue<W> refQueue = new ReferenceQueue<W>();
        
        protected ASegment(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }
        
        @Override
        protected void updateLRU() {
            Reference<? extends W> poll = refQueue.poll();
            if(poll!=null) {
                changeLock.lock();
                try {
                    while(poll!=null) {
                        E pollEntry = (E) poll;
                        if(pollEntry.isValid())
                            removeLRU(pollEntry);
                        poll = refQueue.poll();
                    }
                }
                finally{
                    changeLock.unlock();    
                }
            }

            super.updateLRU();
        }
    }

    static abstract class AEntry<W, E extends AEntry<W, E>> extends WeakReference<W> implements ALRUMap.AEntry<E> {
        protected E next;
        private E before, after;
                
        public int time;

        public AEntry(W weak, ReferenceQueue<W> refQueue, E n, int t) {
            super(weak, refQueue);
            next = n;
            time = t;
        }

        public E getNext() {
            return next;
        }

        public void setNext(E next) {
            this.next = next;
        }

        public E getBefore() {
            return before;
        }

        public void setBefore(E before) {
            this.before = before;
        }

        public E getAfter() {
            return after;
        }

        public void setAfter(E after) {
            this.after = after;
        }

        public int getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = time;
        }

        public void removeFromLRU() {
            before.after = after;
            after.before = before;
            after = before = null;
        }

        public boolean isValid() {
            return after != null;
        }

        public void addBeforeLRU(E existingEntry) {
            after  = existingEntry;
            before = existingEntry.before;
            before.after = (E) this;
            after.before = (E) this;
        }
    }
}
