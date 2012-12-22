package platform.server.caches.hash;

import platform.base.GlobalObject;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;
import platform.server.data.translator.MapTranslate;

public class HashMapKeys implements HashKeys {

    private ImMap<KeyExpr, ? extends GlobalObject> hashKeys;
    public HashMapKeys(ImMap<KeyExpr, ? extends GlobalObject> hashKeys) {
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

    public HashKeys filterKeys(ImSet<KeyExpr> keys) {
        return new HashMapKeys(hashKeys.filter(keys)); // вообще InclKeys но могут быть PullExpr'ы
    }

    public HashKeys reverseTranslate(MapTranslate translator, ImSet<KeyExpr> keys) {
        return new HashMapKeys(translator.translateKey(keys.toRevMap()).join(hashKeys));
    }

    public boolean isGlobal() {
        return false;
    }
}
