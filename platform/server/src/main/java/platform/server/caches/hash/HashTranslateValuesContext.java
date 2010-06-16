package platform.server.caches.hash;

import platform.server.caches.NoCacheInterface;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.MapValuesTranslate;

public class HashTranslateValuesContext implements HashContext, NoCacheInterface {

    private final MapValuesTranslate map;
    public HashTranslateValuesContext(MapValuesTranslate map) {
        this.map = map;
    }

    public int hash(KeyExpr expr) {
        return hashMapKey;
    }

    public int hash(ValueExpr expr) {
        return map.translate(expr).hashCode();
    }

    public HashContext mapKeys() {
        return this;
    }
}
