package platform.server.caches;

import platform.base.*;
import platform.server.Settings;
import platform.server.data.expr.KeyExpr;

import java.util.Map;

public class KeyPairs extends GroupPairs<GlobalObject, KeyExpr, Map<KeyExpr, KeyExpr>> {

    protected Map<KeyExpr, KeyExpr> createI(Map<KeyExpr, KeyExpr> map) {
        return map;
    }

    public KeyPairs(QuickMap<KeyExpr, GlobalObject> map1, QuickMap<KeyExpr, GlobalObject> map2) {
        super(map1, map2, true, Settings.instance.getMapInnerMaxIterations());
    }
}
