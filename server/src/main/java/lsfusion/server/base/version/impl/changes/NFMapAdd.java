package lsfusion.server.base.version.impl.changes;

import lsfusion.base.col.interfaces.mutable.MMap;

import java.util.List;

public class NFMapAdd<K, V> implements NFOrderMapChange<K, V>, NFMapChange<K, V> {
    public final K key;
    public final V value;

    public NFMapAdd(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void proceedOrderMap(List<K> keysList, List<V> valuesList) {
        if(!keysList.contains(key)) {
            keysList.add(key);
            valuesList.add(value);
        }
    }

    @Override
    public void proceedMap(MMap<K, V> map) {
        map.add(key, value);
    }
}
