package platform.server.data.query;

import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.ValueExpr;

public interface HashContext {

    int hash(KeyExpr expr);
    int hash(ValueExpr expr);

}
