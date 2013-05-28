package platform.server.caches.hash;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.ParamExpr;
import platform.server.data.translator.MapTranslate;

public interface HashKeys {

    int hash(ParamExpr expr);

    boolean isGlobal();

    HashKeys filterKeys(ImSet<ParamExpr> keys);

    HashKeys reverseTranslate(MapTranslate translator, ImSet<ParamExpr> keys);
}
