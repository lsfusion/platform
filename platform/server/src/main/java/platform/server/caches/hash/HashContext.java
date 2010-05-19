package platform.server.caches.hash;

import platform.server.data.expr.KeyExpr;

public interface HashContext extends HashValues {

    public final static int hashMapKey = 153;
    public final static int hashMapValue = 155;

    int hash(KeyExpr expr);

}
