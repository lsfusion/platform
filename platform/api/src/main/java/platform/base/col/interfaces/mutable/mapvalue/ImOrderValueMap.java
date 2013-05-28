package platform.base.col.interfaces.mutable.mapvalue;

import platform.base.col.interfaces.immutable.ImOrderMap;

public interface ImOrderValueMap<K, V> {
    
    void mapValue(int i, V value);
        
    ImOrderMap<K, V> immutableValueOrder();
}
