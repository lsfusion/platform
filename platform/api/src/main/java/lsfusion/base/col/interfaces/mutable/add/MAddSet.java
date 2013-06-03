package lsfusion.base.col.interfaces.mutable.add;

import lsfusion.base.col.interfaces.immutable.ImSet;

public interface MAddSet<K> extends Iterable<K> {

    int size();
    boolean isEmpty();
    K get(int i);

    ImSet<K> immutableCopy();
    boolean add(K element);
    void addAll(ImSet<? extends K> set);
    
    boolean contains(K element);
    boolean containsAll(ImSet<? extends K> element);
}
