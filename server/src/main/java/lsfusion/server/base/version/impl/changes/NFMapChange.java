package lsfusion.server.base.version.impl.changes;

import lsfusion.base.col.interfaces.mutable.MMap;

public interface NFMapChange<K, V> {

    void proceedMap(MMap<K, V> map);
}
