package platform.server.caches.hash;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.ParamExpr;
import platform.server.data.translator.MapTranslate;

public class HashCodeKeys implements HashKeys {

    private HashCodeKeys() {
    }
    public static final HashCodeKeys instance = new HashCodeKeys();

    public int hash(ParamExpr expr) {
        return expr.hashCode();
    }

    public HashKeys filterKeys(ImSet<ParamExpr> keys) {
        return this;
    }

    public HashKeys reverseTranslate(MapTranslate translator, ImSet<ParamExpr> keys) {
        if(translator.identityKeys(keys))
            return this;
        else
            return null;
    }

    public boolean isGlobal() {
        return true;
    }
}
