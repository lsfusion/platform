package platform.server.caches.hash;

import platform.server.data.expr.KeyExpr;

public interface HashKeys {

    public boolean isGlobal();

    int hash(KeyExpr expr);
}
