package platform.server.caches.hash;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;

public class HashMapContext implements HashContext {

    private HashMapContext() {
    }
    public final static HashContext instance = new HashMapContext();

    public int hash(KeyExpr expr) {
        return hashMapKey;
    }

    public int hash(ValueExpr expr) {
        return hashMapValue;
    }

    public HashContext mapKeys() {
        return this;
    }
}
