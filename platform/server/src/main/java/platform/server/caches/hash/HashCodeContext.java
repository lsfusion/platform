package platform.server.caches.hash;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;

public class HashCodeContext implements HashContext {

    private HashCodeContext() {    
    }
    public final static HashContext instance = new HashCodeContext();

    public int hash(KeyExpr expr) {
        return expr.hashCode();
    }

    public int hash(ValueExpr expr) {
        return expr.hashCode();
    }

    public HashContext mapKeys() {
        return HashMapKeysContext.instance;
    }
}
