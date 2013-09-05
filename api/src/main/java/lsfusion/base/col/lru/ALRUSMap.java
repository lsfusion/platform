package lsfusion.base.col.lru;

public abstract class ALRUSMap<E extends ALRUSMap.AEntry<E>, S extends ALRUMap.ASegment> extends ALRUMap<E, S> {

    protected ALRUSMap(LRUUtil.Strategy expireStrategy) {
        super(expireStrategy);
    }

    static abstract class AEntry<E extends AEntry<E>> implements ALRUMap.AEntry<E> {
        protected E next;
        private E before, after;

        public int time;

        public AEntry(E n, int t) {
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
