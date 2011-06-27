package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.server.caches.ParamLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.Collections;
import java.util.Map;

public class MaxGroupExpr extends GroupExpr {

    protected MaxGroupExpr(Map<BaseExpr, BaseExpr> group, Expr expr) {
        super(group, expr);
    }

    protected MaxGroupExpr(MaxGroupExpr maxExpr, MapTranslate translator) {
        super(maxExpr, translator);
    }

    protected MaxGroupExpr createThis(Expr query, Map<BaseExpr, BaseExpr> group) {
        return new MaxGroupExpr(group, query);
    }

    @ParamLazy
    public MaxGroupExpr translateOuter(MapTranslate translator) {
        return new MaxGroupExpr(this,translator);
    }

    @Override
    public GroupType getGroupType() {
        return GroupType.MAX;
    }

    public Where calculateWhere() {
        return new NotNull();
    }

    protected class NotNull extends GroupExpr.NotNull {

        protected ClassExprWhere getClassWhere(Where fullWhere) {
            return fullWhere.getClassWhere().map(BaseUtils.merge(Collections.singletonMap(query.getBaseExpr(), MaxGroupExpr.this), group));
        }
    }

}
