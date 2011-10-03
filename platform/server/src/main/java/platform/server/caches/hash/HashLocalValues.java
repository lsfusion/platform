package platform.server.caches.hash;

import platform.base.BaseUtils;
import platform.server.caches.InnerHashContext;
import platform.server.data.expr.KeyExpr;

import java.util.IdentityHashMap;
import java.util.Map;

public abstract class HashLocalValues extends HashValues {

    private IdentityHashMap<InnerHashContext, BaseUtils.HashComponents<KeyExpr>> cacheComponents;
    public IdentityHashMap<InnerHashContext, BaseUtils.HashComponents<KeyExpr>> getCacheComponents() {
        if(cacheComponents==null)
            cacheComponents = new IdentityHashMap<InnerHashContext, BaseUtils.HashComponents<KeyExpr>>();
        return cacheComponents;
    }

    public boolean isGlobal() {
        return false;
    }
}
