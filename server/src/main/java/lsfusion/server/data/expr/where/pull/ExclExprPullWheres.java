package lsfusion.server.data.expr.where.pull;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;

public abstract class ExclExprPullWheres<R> extends ExclPullWheres<R, Integer, Where> {

    protected R proceedBase(Where data, ImMap<Integer, BaseExpr> map) {
        return proceedBase(data, map.get(0));
    }

    protected abstract R proceedBase(Where data, BaseExpr baseExpr);

    public R proceed(Where data, Expr expr) {
        return proceed(data, MapFact.singleton(0, expr));
    }
}
