package lsfusion.server.base.caches.hash;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.caches.ParamExpr;
import lsfusion.server.data.translator.MapTranslate;

public interface HashKeys {

    int hash(ParamExpr expr);

    boolean isGlobal();

    HashKeys filterKeys(ImSet<ParamExpr> keys);

    HashKeys reverseTranslate(MapTranslate translator, ImSet<ParamExpr> keys);
}
