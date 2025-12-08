package lsfusion.server.base.version.interfaces;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NF;

import java.util.function.Function;

public interface NFMap<K, V> extends NF {

    void add(K key, V value, Version version);
    void add(NFMap<K, V> map, Function<V, V> mapping, Version version);

    ImMap<K, V> getNF(Version version);
    ImMap<K, V> getMap();

    V getNFValue(K key, Version version);
}
