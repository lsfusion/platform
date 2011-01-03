package platform.server.caches;

import platform.base.GroupPairs;
import platform.base.BaseUtils;
import platform.base.GlobalInteger;
import platform.base.GlobalObject;
import platform.server.Settings;
import platform.server.data.expr.KeyExpr;

import java.util.Map;

public class KeyPairs extends GroupPairs<GlobalObject, KeyExpr, Map<KeyExpr, KeyExpr>> {

    protected Map<KeyExpr, KeyExpr> createI(Map<KeyExpr, KeyExpr> map) {
        return map;
    }

    public KeyPairs(Map<KeyExpr, GlobalObject> map1, Map<KeyExpr, GlobalObject> map2) {
        super(map1, map2, true, Settings.instance.getMapInnerMaxIterations());
    }
}
