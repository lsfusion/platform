package lsfusion.server.base.version.impl.changes;

import lsfusion.server.base.version.Version;

import java.util.List;

// a key the map has gets the value and keeps its place, a new one is added
public class NFMapSet<K, V> implements NFOrderMapChange<K, V> {
    private final K key;
    private final V value;

    public NFMapSet(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void proceedOrderMap(List<K> keysList, List<V> valuesList, Version version) {
        int index = keysList.indexOf(key);
        if(index >= 0)
            valuesList.set(index, value);
        else {
            keysList.add(key);
            valuesList.add(value);
        }
    }
}
