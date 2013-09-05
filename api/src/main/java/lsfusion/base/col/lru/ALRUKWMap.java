package lsfusion.base.col.lru;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

// у кого ключ в weak придется сохранять хэш, потому как иначе не найдешь в таблице 
public abstract class ALRUKWMap<W, E extends ALRUKWMap.AEntry<W, E>, S extends ALRUKWMap.ASegment> extends ALRUWMap<W, E, S> {

    protected ALRUKWMap(LRUUtil.Strategy expireStrategy) {
        super(expireStrategy);
    }

    static abstract class AEntry<W, E extends AEntry<W, E>> extends ALRUWMap.AEntry<W, E> implements ALRUMap.AEntry<E> {
        protected final int hash;

        public AEntry(W weak, ReferenceQueue<W> refQueue, E n, int hash, int t) {
            super(weak, refQueue, n, t);
            this.hash = hash;
        }

        public int hashKey() {
            return hash;
        }
    }
}
