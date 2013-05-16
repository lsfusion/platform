package platform.server.data.expr.where.pull;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;

public abstract class ExclExprPullWheres<R> extends ExclPullWheres<R, Integer, Where> {

    protected R proceedBase(Where data, ImMap<Integer, BaseExpr> map) {
        return proceedBase(data, map.get(0));
    }

    protected abstract R proceedBase(Where data, BaseExpr baseExpr);

    public R proceed(Where data, Expr expr) {
        return proceed(data, MapFact.singleton(0, expr));
    }
}
