package platform.server.caches;

import platform.server.data.expr.ValueExpr;

public interface HashValues {

    int hash(ValueExpr expr);
}
