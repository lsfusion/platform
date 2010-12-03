package platform.server.caches.hash;

import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.KeyExpr;

import java.util.Map;

public class HashMapValues implements HashValues {

    private Map<ValueExpr, Integer> hashValues;
    public HashMapValues(Map<ValueExpr, Integer> hashValues) {
        this.hashValues = hashValues;
    }

    public int hash(ValueExpr expr) {
        return hashValues.get(expr);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HashMapValues && hashValues.equals(((HashMapValues) o).hashValues);
    }

    @Override
    public int hashCode() {
        return hashValues.hashCode();
    }
}
