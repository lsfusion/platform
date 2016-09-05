package lsfusion.server.data.expr.where.pull;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.where.Where;

public class StatPullWheres extends ExclPullWheres<Stat, Integer, Where> {

    private final StatType statType;
    public StatPullWheres(StatType statType) {
        this.statType = statType;
    }

    protected Stat proceedBase(Where data, ImMap<Integer, BaseExpr> map) {
        BaseExpr baseExpr = map.get(0);
        StatKeys<BaseExpr> statKeys = data.getStatKeys(SetFact.<BaseExpr>singleton(baseExpr), statType);
        Stat result = statKeys.getDistinct(baseExpr);
        assert BaseUtils.hashEquals(result, statKeys.getRows());
        return result;
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
