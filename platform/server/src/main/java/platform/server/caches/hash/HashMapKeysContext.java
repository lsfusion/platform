package platform.server.caches.hash;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;

public class HashMapKeysContext implements HashContext {

    private HashMapKeysContext() {
    }
    public final static HashContext instance = new HashMapKeysContext();

    public int hash(KeyExpr expr) {
        return 1;
    }

    public int hash(ValueExpr expr) {
        return expr.hashCode();
    }

    public HashContext mapKeys() {
        return this;
    }
}
