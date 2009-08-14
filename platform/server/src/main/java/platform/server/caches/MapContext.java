package platform.server.caches;

import platform.server.data.query.Context;
import platform.server.data.query.HashContext;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.ValueExpr;

import java.util.Map;

public interface MapContext {
    
    int hash(HashContext hashContext);

    Context getContext();
}
