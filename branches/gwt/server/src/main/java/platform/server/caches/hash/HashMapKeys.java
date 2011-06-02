package platform.server.caches.hash;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;
import platform.base.BaseUtils;
import platform.base.GlobalObject;

import java.util.Map;

public class HashMapKeys implements HashKeys {

    private Map<KeyExpr, ? extends GlobalObject> hashKeys;
    public HashMapKeys(Map<KeyExpr, ? extends GlobalObject> hashKeys) {
        this.hashKeys = hashKeys;
    }

    public int hash(KeyExpr expr) {
        GlobalObject hash = hashKeys.get(expr);
        if(hash==null) {
            assert expr instanceof PullExpr;
            return expr.hashCode();
        } else
            return hash.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HashMapKeys && hashKeys.equals(((HashMapKeys) o).hashKeys);
    }

    @Override
    public int hashCode() {
        return hashKeys.hashCode();
    }
}
