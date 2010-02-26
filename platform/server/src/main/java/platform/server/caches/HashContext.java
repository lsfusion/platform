package platform.server.caches;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;

public interface HashContext {

    int hash(KeyExpr expr);
    int hash(ValueExpr expr);

}
