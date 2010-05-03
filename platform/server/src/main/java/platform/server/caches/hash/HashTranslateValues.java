package platform.server.caches.hash;

import platform.server.data.expr.ValueExpr;
import platform.server.caches.NoCacheInterface;

import java.util.Map;

public class HashTranslateValues implements HashValues, NoCacheInterface {

    private final Map<ValueExpr,ValueExpr> map;

    public HashTranslateValues(Map<ValueExpr, ValueExpr> map) {
        this.map = map;
    }

    public int hash(ValueExpr expr) {
        return map.get(expr).hashCode();
    }

    public HashContext mapKeys() {
        return new HashTranslateValuesContext(map);
    }
}
