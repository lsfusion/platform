package lsfusion.server.data.expr.join.select;

import lsfusion.base.col.SetFact;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.stat.KeyStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatKeys;
import lsfusion.server.data.stat.StatType;

public class ExprEqualsJoin extends ExprCompareJoin<BaseExpr, ExprEqualsJoin> {

    public ExprEqualsJoin(BaseExpr expr1, BaseExpr expr2) {
        super(expr1, expr2);
    }

    @Override
    protected ExprEqualsJoin createThis(BaseExpr expr1, BaseExpr expr2) {
        return new ExprEqualsJoin(expr1, expr2);
    }

    public StatKeys<Integer> getStatKeys(KeyStat keyStat, StatType type) { // тут по идее forJoin и true и false подойдут
        return new StatKeys<>(SetFact.toExclSet(0, 1), expr1.getTypeStat(keyStat, false).min(expr2.getTypeStat(keyStat, false)));
    }
}
