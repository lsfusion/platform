package lsfusion.base.col.implementations.abs;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.col.interfaces.mutable.MOrderFilterMap;
import lsfusion.base.col.interfaces.mutable.MOrderMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;

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

    protected abstract MOrderExclMap<K, V> orderCopy();

    public ImOrderMap<K, V> immutableOrderCopy() {
        return orderCopy().immutableOrder();
    }
}
