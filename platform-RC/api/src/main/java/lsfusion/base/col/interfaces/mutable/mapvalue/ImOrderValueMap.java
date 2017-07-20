package lsfusion.base.col.interfaces.mutable.mapvalue;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;

public interface ImOrderValueMap<K, V> {
    
    void mapValue(int i, V value);
        
    ImOrderMap<K, V> immutableValueOrder();
}
