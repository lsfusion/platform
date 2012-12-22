package platform.base.col.interfaces.mutable;

import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.immutable.ImSet;

public interface MSet<K> {

    boolean add(K element);
    
    void addAll(ImSet<? extends K> col);
    
    boolean contains(K element);
    int size();
    
    ImSet<K> immutable();
}
