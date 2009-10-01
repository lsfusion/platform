package platform.server.caches;

import platform.server.data.query.Context;
import platform.server.data.query.HashContext;

public interface MapContext {
    
    int hash(HashContext hashContext);

    Context getContext();
}
