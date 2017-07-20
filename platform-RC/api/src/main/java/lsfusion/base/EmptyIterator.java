package lsfusion.base;

import java.util.Iterator;

public class EmptyIterator<T> implements Iterator<T> {

    public boolean hasNext() {
        return false;
    }

    public T next() {
        return null;
    }

    public void remove() {
    }
}
