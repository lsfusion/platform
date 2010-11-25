package platform.server.caches;

import platform.base.GroupPairs;
import platform.server.data.expr.KeyExpr;
import platform.server.Settings;

import java.util.Map;

public class KeyPairs extends GroupPairs<Integer, KeyExpr, Map<KeyExpr, KeyExpr>> {

    protected Map<KeyExpr, KeyExpr> createI(Map<KeyExpr, KeyExpr> map) {
        return map;
    }

    public KeyPairs(Map<KeyExpr, Integer> map1, Map<KeyExpr, Integer> map2) {
        super(map1, map2, true, Settings.instance.getMapInnerMaxIterations());
    }
}
