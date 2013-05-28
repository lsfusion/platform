package platform.base.col.implementations.abs;

import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.mutable.MOrderExclMap;
import platform.base.col.interfaces.mutable.MOrderFilterMap;
import platform.base.col.interfaces.mutable.MOrderMap;
import platform.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;

public abstract class AMOrderMap<K, V> extends AOrderMap<K, V> implements MOrderMap<K, V>, MOrderExclMap<K, V>, ImOrderValueMap<K, V>, MOrderFilterMap<K, V> {

    public void addAll(ImOrderMap<? extends K, ? extends V> map) {
        for(int i=0,size=map.size();i<size;i++)
            exclAdd(map.getKey(i), map.getValue(i));
    }

    public void exclAddAll(ImOrderMap<? extends K, ? extends V> map) {
        for(int i=0,size=map.size();i<size;i++)
            exclAdd(map.getKey(i), map.getValue(i));
    }

    public ImOrderMap<K, V> immutableValueOrder() {
        return this;
    }

    public void keep(K key, V value) {
        exclAdd(key, value);
    }
}
