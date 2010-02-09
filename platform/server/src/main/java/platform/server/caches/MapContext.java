package platform.server.caches;

import platform.server.data.query.HashContext;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;

import java.util.Set;

public interface MapContext {
    
    int hash(HashContext hashContext);

    Set<KeyExpr> getKeys();
    Set<ValueExpr> getValues();
}
