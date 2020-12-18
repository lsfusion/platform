package lsfusion.server.base.version.impl.changes;

import java.util.List;

public interface NFOrderMapChange<K, V> {
    
    void proceedOrderMap(List<K> keysList, List<V> valuesList);
}
