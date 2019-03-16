package lsfusion.server.data.caches;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.comb.GroupPairs;
import lsfusion.base.comb.map.GlobalObject;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.physics.admin.Settings;

public class KeyPairs extends GroupPairs<GlobalObject, ParamExpr, ImRevMap<ParamExpr, ParamExpr>> {

    protected ImRevMap<ParamExpr, ParamExpr> createI(ImRevMap<ParamExpr, ParamExpr> map) {
        return map;
    }

    public KeyPairs(ImMap<ParamExpr, GlobalObject> map1, ImMap<ParamExpr, GlobalObject> map2) {
        super(map1, map2, true, Settings.get().getMapInnerMaxIterations());
    }
}
