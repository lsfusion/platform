package lsfusion.server.base.version.impl.changes;

import lsfusion.server.base.version.FindIndex;

import java.util.List;

// assert что есть
public class NFMapMove<K, V> implements NFOrderMapChange<K, V> {
    private final K key;
    private final V value;
    private final FindIndex<K> finder;

    public NFMapMove(K key, V value, FindIndex<K> finder) {
        this.key = key;
        this.value = value;
        this.finder = finder;
    }

    @Override
    public void proceedOrderMap(List<K> keysList, List<V> valuesList) {
        int index = keysList.indexOf(key);
        if(index >= 0) {
            keysList.remove(index);
            valuesList.remove(index);
        }
        index = finder.getIndex(keysList);
        keysList.add(index, key);
        valuesList.add(index, value);
    }
}
