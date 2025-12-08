package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImList;
import java.util.function.Predicate;

public interface MList<K> {

    void add(K key);

    void addFirst(K key);

    int size();
    void set(int i, K value);
    K get(int i);
    
    // hacks for extremely rear cases
    void removeAll();
    void removeAll(Predicate<? super K> filter);
    void removeLast();

    void addAll(ImList<? extends K> list);
        
    ImList<K> immutableList();
}
