package platform.server.caches.hash;

import platform.server.data.expr.ValueExpr;

public interface HashValues {

    public final static int hashMapValue = 155;
    
    int hash(ValueExpr expr);

    HashContext mapKeys();
}
