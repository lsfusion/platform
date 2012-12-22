package platform.base.col.interfaces.mutable.add;

import platform.base.col.interfaces.immutable.ImSet;

public interface MAddSet<K> {

    Iterable<K> it(); // редкое использование поэтому не extends

    ImSet<K> immutableCopy();
    boolean add(K element);
    void addAll(ImSet<? extends K> set);
    
    boolean contains(K element);
    boolean containsAll(ImSet<? extends K> element);
}
