package platform.server.caches.hash;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.MapTranslate;

public class HashCodeKeys implements HashKeys {

    private HashCodeKeys() {
    }
    public static final HashCodeKeys instance = new HashCodeKeys();

    public int hash(KeyExpr expr) {
        return expr.hashCode();
    }

    public HashKeys filterKeys(ImSet<KeyExpr> keys) {
        return this;
    }

    public HashKeys reverseTranslate(MapTranslate translator, ImSet<KeyExpr> keys) {
        if(translator.identityKeys(keys))
            return this;
        else
            return null;
    }

    public boolean isGlobal() {
        return true;
    }
}
