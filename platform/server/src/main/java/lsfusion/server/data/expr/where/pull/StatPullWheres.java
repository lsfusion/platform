package lsfusion.server.data.expr.where.pull;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.where.Where;

public class StatPullWheres extends ExclPullWheres<Stat, Integer, Where> {

    protected Stat proceedBase(Where data, ImMap<Integer, BaseExpr> map) {
        BaseExpr baseExpr = map.get(0);
        return data.getStatKeys(SetFact.<BaseExpr>singleton(baseExpr)).rows;
    }

    protected Stat initEmpty() {
        return Stat.MIN;
    }

    protected Stat add(Stat op1, Stat op2) {
        return op1.or(op2);
    }

    public Stat proceed(Where where, Expr expr) {
        return proceed(where, MapFact.singleton(0, expr));
    }
}
