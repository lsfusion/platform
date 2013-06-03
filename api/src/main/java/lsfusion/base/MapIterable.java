package lsfusion.base;

import java.util.Iterator;

public abstract class MapIterable<K,M> implements Iterable<M> {

    protected abstract M map(K key);
    protected abstract Iterator<K> mapIterator();

    public Iterator<M> iterator() {
        return new MapIterator();
    }

    private class MapIterator implements Iterator<M> {
        protected Iterator<K> mapIterator;

        private MapIterator() {
            mapIterator = mapIterator();
        }

        M next;
        public boolean hasNext() {
            while(next==null) {
                if(!mapIterator.hasNext())
                    return false;
                next = map(mapIterator.next());
            }
            return true;
        }

        public M next() {
            if(!hasNext())
                throw new RuntimeException("no next");

            M result = next;
            next = null;
            return result;
        }

        public void remove() {
            throw new RuntimeException("not supported");
        }
    }
}
