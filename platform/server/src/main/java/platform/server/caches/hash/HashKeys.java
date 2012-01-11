package platform.server.caches.hash;

import platform.base.QuickSet;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.MapTranslate;

public interface HashKeys {

    int hash(KeyExpr expr);

    boolean isGlobal();

    HashKeys filterKeys(QuickSet<KeyExpr> keys);

    HashKeys reverseTranslate(MapTranslate translator, QuickSet<KeyExpr> keys);
}
