package platform.server.caches.hash;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;

import java.util.Map;

public class HashMapKeys implements HashKeys {

    private Map<KeyExpr, Integer> hashKeys;
    public HashMapKeys(Map<KeyExpr, Integer> hashKeys) {
        this.hashKeys = hashKeys;
    }

    public int hash(KeyExpr expr) {
        Integer hash = hashKeys.get(expr);
        if(hash==null) {
            assert expr instanceof PullExpr;
            return expr.hashCode();
        } else
            return hash;
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
