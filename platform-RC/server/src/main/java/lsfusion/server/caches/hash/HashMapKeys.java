package lsfusion.server.caches.hash;

import lsfusion.base.GlobalObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.Settings;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.PullExpr;
import lsfusion.server.data.translator.MapTranslate;

public class HashMapKeys implements HashKeys {

    public static HashKeys create(ImMap<ParamExpr, ? extends GlobalObject> hashKeys) {
        if(hashKeys.isEmpty())
            return HashCodeKeys.instance;
        return new HashMapKeys(hashKeys);
    }
    private ImMap<ParamExpr, ? extends GlobalObject> hashKeys;
    private HashMapKeys(ImMap<ParamExpr, ? extends GlobalObject> hashKeys) {
        this.hashKeys = hashKeys;
    }

    public int hash(ParamExpr expr) {
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

    public HashKeys filterKeys(ImSet<ParamExpr> keys) {
        return create(hashKeys.filter(keys)); // вообще InclKeys но могут быть PullExpr'ы
    }

    public HashKeys reverseTranslate(MapTranslate translator, ImSet<ParamExpr> keys) {
        return create(translator.translateKey(keys.toRevMap()).join(hashKeys));
    }

    public boolean isGlobal() {
        return Settings.get().isCacheInnerHashes();
    }
}
