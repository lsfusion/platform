package platform.server.caches.hash;

import platform.server.data.expr.KeyExpr;

public class HashCodeKeys implements HashKeys {

    private HashCodeKeys() {
    }
    public static final HashCodeKeys instance = new HashCodeKeys();

    public boolean isGlobal() {
        return true;
    }

    public int hash(KeyExpr expr) {
        return expr.hashCode();
    }
}
