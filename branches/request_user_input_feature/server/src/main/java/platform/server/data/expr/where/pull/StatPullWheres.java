package platform.server.data.expr.where.pull;

import platform.base.QuickSet;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.Stat;
import platform.server.data.where.Where;

import java.util.Collections;
import java.util.Map;

public class StatPullWheres extends ExclPullWheres<Stat, Integer, Where> {

    protected Stat proceedBase(Where data, Map<Integer, BaseExpr> map) {
        BaseExpr baseExpr = map.get(0);
        return data.getStatKeys(new QuickSet<BaseExpr>(baseExpr)).rows;
    }

    protected Stat initEmpty() {
        return Stat.MIN;
    }

    protected Stat add(Stat op1, Stat op2) {
        return op1.or(op2);
    }

    public Stat proceed(Where where, Expr expr) {
        return proceed(where, Collections.singletonMap(0, expr));
    }
}
