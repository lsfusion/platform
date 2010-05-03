package platform.server.caches.hash;

import platform.server.data.expr.ValueExpr;

public class HashCodeValues implements HashValues {

    private HashCodeValues() {
    }
    public final static HashValues instance = new HashCodeValues();

    public int hash(ValueExpr expr) {
        return expr.hashCode();
    }

    public HashContext mapKeys() {
        return HashMapKeysContext.instance;
    }
}
