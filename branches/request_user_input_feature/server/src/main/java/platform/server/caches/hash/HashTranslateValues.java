package platform.server.caches.hash;

import platform.base.GlobalInteger;
import platform.base.ImmutableObject;
import platform.base.QuickSet;
import platform.server.caches.NoCacheInterface;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HashTranslateValues extends HashValues {

    private final MapValuesTranslate map;

    public HashTranslateValues(MapValuesTranslate map) {
        this.map = map;
    }

    public int hash(Value expr) {
        return map.translate(expr).hashCode();
    }

    public boolean equals(Object o) {
        return this == o || o instanceof HashTranslateValues && map.equals(((HashTranslateValues) o).map);
    }

    public int hashCode() {
        return map.hashCode();
    }

    public boolean isGlobal() {
        return false;
    }

    public HashValues filterValues(QuickSet<Value> values) {
        throw new RuntimeException("not supported");
    }

    public HashValues reverseTranslate(MapValuesTranslate translate, QuickSet<Value> values) {
        throw new RuntimeException("not supported");
    }
}
