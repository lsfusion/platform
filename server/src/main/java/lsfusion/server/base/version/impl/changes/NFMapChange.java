package lsfusion.server.base.version.impl.changes;

import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.base.version.Version;

public interface NFMapChange<K, V> {

    void proceedMap(MMap<K, V> map, Version version);
}
