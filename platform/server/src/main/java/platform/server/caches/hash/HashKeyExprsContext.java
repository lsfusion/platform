package platform.server.caches.hash;

import platform.server.caches.NoCacheInterface;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;

import java.util.Map;

public class HashKeyExprsContext implements HashContext, NoCacheInterface {

    private final Map<KeyExpr, BaseExpr> keyExprs;

    public HashKeyExprsContext(Map<KeyExpr, BaseExpr> keyExprs) {
        this.keyExprs = keyExprs;
    }

    public int hash(KeyExpr expr) {
        BaseExpr keyExpr = keyExprs.get(expr);
        if(keyExpr==null)
            return expr.hashCode();
        else
            return keyExpr.hashCode(); 
    }

    public int hash(ValueExpr expr) {
        return expr.hashCode();
    }

    public HashContext mapKeys() {
        return HashMapKeysContext.instance;
    }
}
