package platform.server.caches;

import platform.base.GlobalObject;
import platform.base.GroupPairs;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.Settings;
import platform.server.data.expr.KeyExpr;

public class KeyPairs extends GroupPairs<GlobalObject, KeyExpr, ImRevMap<KeyExpr, KeyExpr>> {

    protected ImRevMap<KeyExpr, KeyExpr> createI(ImRevMap<KeyExpr, KeyExpr> map) {
        return map;
    }

    public KeyPairs(ImMap<KeyExpr, GlobalObject> map1, ImMap<KeyExpr, GlobalObject> map2) {
        super(map1, map2, true, Settings.get().getMapInnerMaxIterations());
    }
}
