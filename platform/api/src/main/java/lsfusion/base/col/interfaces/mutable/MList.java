package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImList;

public interface MList<K> {

    void add(K key);
    int size();
    void set(int i, K value);
    K get(int i);

    void addAll(ImList<? extends K> list);
        
    ImList<K> immutableList();
}
