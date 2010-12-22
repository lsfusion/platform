package platform.server.caches.hash;

import platform.server.caches.NoCacheInterface;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;

public class HashTranslateValues implements HashValues, NoCacheInterface {

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
}
