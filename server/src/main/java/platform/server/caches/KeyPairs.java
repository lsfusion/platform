package platform.server.caches;

import platform.base.GlobalObject;
import platform.base.GroupPairs;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.Settings;

public class KeyPairs extends GroupPairs<GlobalObject, ParamExpr, ImRevMap<ParamExpr, ParamExpr>> {

    protected ImRevMap<ParamExpr, ParamExpr> createI(ImRevMap<ParamExpr, ParamExpr> map) {
        return map;
    }

    public KeyPairs(ImMap<ParamExpr, GlobalObject> map1, ImMap<ParamExpr, GlobalObject> map2) {
        super(map1, map2, true, Settings.get().getMapInnerMaxIterations());
    }
}
