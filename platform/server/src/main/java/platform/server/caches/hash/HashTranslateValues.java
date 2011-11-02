package platform.server.caches.hash;

import platform.base.GlobalInteger;
import platform.base.ImmutableObject;
import platform.server.caches.NoCacheInterface;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HashTranslateValues extends HashLocalValues {

    private final MapValuesTranslate map;

    public HashTranslateValues(MapValuesTranslate map) {
        this.map = map;
    }

    public int hash(Value expr) {
        return map.translate(expr).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HashTranslateValues && map.equals(((HashTranslateValues) o).map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public HashValues filterValues(Set<Value> values) {
        Map<Value, GlobalInteger> mapHashes = new HashMap<Value, GlobalInteger>();
        for(Value value : values)
            mapHashes.put(value, new GlobalInteger(hash(value)));
        return new HashMapValues(mapHashes);
    }
}
