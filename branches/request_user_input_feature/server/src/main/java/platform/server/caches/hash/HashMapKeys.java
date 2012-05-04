package platform.server.caches.hash;

import platform.base.*;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;
import platform.server.data.translator.MapTranslate;

public class HashMapKeys implements HashKeys {

    private QuickMap<KeyExpr, ? extends GlobalObject> hashKeys;
    public HashMapKeys(QuickMap<KeyExpr, ? extends GlobalObject> hashKeys) {
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

    @Override
    public HashKeys filterKeys(QuickSet<KeyExpr> keys) {
        return new HashMapKeys(hashKeys.filterKeys(keys)); // вообще InclKeys но могут быть PullExpr'ы
    }

    public HashKeys reverseTranslate(MapTranslate translator, QuickSet<KeyExpr> keys) {
        QuickMap<KeyExpr, GlobalObject> transKeys = new SimpleMap<KeyExpr, GlobalObject>();
        for(int i=0;i<keys.size;i++) {
            KeyExpr keyExpr = keys.get(i);
            transKeys.add(keyExpr, hashKeys.get(translator.translate(keyExpr)));
        }
        return new HashMapKeys(transKeys);
    }

    public boolean isGlobal() {
        return false;
    }
}
