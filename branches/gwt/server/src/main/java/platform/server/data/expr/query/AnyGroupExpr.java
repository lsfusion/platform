package platform.server.data.expr.query;

import platform.server.caches.ParamLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.translator.MapTranslate;

import java.util.Map;

public class AnyGroupExpr extends MaxGroupExpr {

    public AnyGroupExpr(Map<BaseExpr, BaseExpr> group, Expr expr) {
        super(group, expr);
    }

    private AnyGroupExpr(MaxGroupExpr maxExpr, MapTranslate translator) {
        super(maxExpr, translator);
    }

    @Override
    protected AnyGroupExpr createThis(Expr query, Map<BaseExpr, BaseExpr> group) {
        return new AnyGroupExpr(group, query);
    }

    @Override
    @ParamLazy
    public AnyGroupExpr translateOuter(MapTranslate translator) {
        return new AnyGroupExpr(this, translator);
    }

    @Override
    public GroupType getGroupType() {
        return GroupType.ANY;
    }
}
