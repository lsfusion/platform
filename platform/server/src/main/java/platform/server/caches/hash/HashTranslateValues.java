package platform.server.caches.hash;

import platform.server.caches.NoCacheInterface;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.MapValuesTranslate;

public class HashTranslateValues implements HashValues, NoCacheInterface {

    private final MapValuesTranslate map;

    public HashTranslateValues(MapValuesTranslate map) {
        this.map = map;
    }

    public int hash(ValueExpr expr) {
        return map.translate(expr).hashCode();
    }

    public HashContext mapKeys() {
        return new HashTranslateValuesContext(map);
    }
}
