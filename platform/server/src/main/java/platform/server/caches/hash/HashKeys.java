package platform.server.caches.hash;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.MapTranslate;

public interface HashKeys {

    int hash(KeyExpr expr);

    boolean isGlobal();

    HashKeys filterKeys(ImSet<KeyExpr> keys);

    HashKeys reverseTranslate(MapTranslate translator, ImSet<KeyExpr> keys);
}
