package lsfusion.server.base.version.impl.changes;

import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFMap;

import java.util.function.Function;

public class NFMapCopy<K, V> implements NFMapChange<K, V> {

    public NFMap<K, V> map;
    public Function<V, V> mapping;
    public NFMapCopy(NFMap<K, V> map, Function<V, V> mapping) {
        this.map = map;
        this.mapping = mapping;
    }

    @Override
    public void proceedMap(MMap<K, V> mMap, Version version) {
        mMap.addAll(map.getNF(version).mapValues(mapping));
    }
}
