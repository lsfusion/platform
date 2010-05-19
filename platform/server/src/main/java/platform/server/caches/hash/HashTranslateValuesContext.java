package platform.server.caches.hash;

import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.caches.NoCacheInterface;

import java.util.Map;

public class HashTranslateValuesContext implements HashContext, NoCacheInterface {

    private final Map<ValueExpr,ValueExpr> map;
    public HashTranslateValuesContext(Map<ValueExpr, ValueExpr> map) {
        this.map = map;
    }

    public int hash(KeyExpr expr) {
        return hashMapKey;
    }

    public int hash(ValueExpr expr) {
        return map.get(expr).hashCode();
    }

    public HashContext mapKeys() {
        return this;
    }
}
