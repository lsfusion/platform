package platform.server.caches.hash;

import platform.server.data.expr.KeyExpr;

public interface HashContext extends HashValues {

    int hash(KeyExpr expr);

}
