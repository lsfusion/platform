package lsfusion.base.col.interfaces.mutable;

import lsfusion.base.col.interfaces.immutable.ImCol;
import java.util.function.Predicate;

public interface MCol<K> {

    void add(K key);
    int size();
    
    void removeAll();
    void removeAll(Predicate<? super K> filter);

    void addAll(ImCol<? extends K> col);
    
    ImCol<K> immutableCol();
}
