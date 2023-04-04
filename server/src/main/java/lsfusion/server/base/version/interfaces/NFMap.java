package lsfusion.server.base.version.interfaces;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NF;

public interface NFMap<K, V> extends NF {

    void add(K key, V value, Version version);

    ImMap<K, V> getMap();

    V getNFValue(K key, Version version);
}
