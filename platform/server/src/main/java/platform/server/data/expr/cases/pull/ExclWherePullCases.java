package platform.server.data.expr.cases.pull;

import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;

import java.util.Collections;
import java.util.Map;

public abstract class ExclWherePullCases<R> extends ExclPullCases<R, Integer, Where> {

    @Override
    protected R proceedBase(Where data, Map<Integer, BaseExpr> map) {
        return proceedBase(data, map.get(0));
    }

    protected abstract R proceedBase(Where data, BaseExpr baseExpr);

    public R proceed(Where data, Expr expr) {
        return proceed(data, Collections.singletonMap(0, expr));
    }
}
