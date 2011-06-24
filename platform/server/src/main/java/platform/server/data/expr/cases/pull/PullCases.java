package platform.server.data.expr.cases.pull;

import platform.base.BaseUtils;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.cases.ExprCase;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.cases.MapCaseList;

import java.util.Map;

public abstract class PullCases<R, K> {

    protected abstract R proceedCases(MapCaseList<K> cases);
    protected abstract R proceedBase(Map<K, BaseExpr> map);

    public R proceed(Map<K, ? extends Expr> map) {
        // ищем первый попавшийся, есть второй алгоритм с непроталкиванием скобок
        for(Map.Entry<K, ? extends Expr> entry : map.entrySet())
            if(!(entry.getValue() instanceof BaseExpr)) {
                MapCaseList<K> cases = new MapCaseList<K>();
                for(ExprCase exprCase : entry.getValue().getCases())
                    cases.add(exprCase.where, BaseUtils.replace(map, entry.getKey(), exprCase.data));
                return proceedCases(cases);
            }

        return proceedBase((Map<K, BaseExpr>) (Map<K, ? extends BaseExpr>) map);
    }
}
