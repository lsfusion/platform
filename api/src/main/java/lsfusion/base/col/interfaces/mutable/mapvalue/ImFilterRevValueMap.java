package lsfusion.base.col.interfaces.mutable.mapvalue;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;

public interface ImFilterRevValueMap<K, V> {
    
    void mapValue(int i, V value);

    ImRevMap<K, V> immutableRevValue();
}
