package lsfusion.server.data.query;

import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.Stat;

// нужен пока и в будущем для работы с интервалами, сейчас 3 хака:
// 1. ExprIndexedJoin.fillIntervals - 
// 2. WhereJoins.removeJoin - чтобы не протолкнулся висячий ключ
// 3. WhereJoins.buildGraph - чтобы не протолкнулся висячий ключ

public class ExprIntervalJoin extends ExprStatJoin {

    public ExprIntervalJoin(BaseExpr baseExpr, Stat stat, InnerJoins valueJoins, boolean notNull) {
        super(baseExpr, stat, valueJoins, notNull);
    }
}
