package platform.server.caches.hash;

import platform.server.data.expr.ValueExpr;

public interface HashValues {

    int hash(ValueExpr expr);

    HashContext mapKeys();
}
