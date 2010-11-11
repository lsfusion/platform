package platform.server.caches.hash;

import platform.server.data.expr.KeyExpr;

public interface HashKeys {

    int hash(KeyExpr expr);
}
