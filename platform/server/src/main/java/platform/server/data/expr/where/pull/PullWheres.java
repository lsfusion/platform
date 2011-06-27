package platform.server.data.expr.where.pull;

import platform.base.BaseUtils;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.MapCaseList;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.where.ifs.NullExpr;
import platform.server.data.expr.where.ifs.IfExpr;
import platform.server.data.where.Where;

import java.util.Map;

public abstract class PullWheres<R, K> {

    protected abstract R proceedCases(MapCaseList<K> cases);
    protected abstract R proceedIf(Where ifWhere, Map<K, Expr> mapTrue, Map<K, Expr> mapFalse);
    protected abstract R proceedBase(Map<K, BaseExpr> map);

    protected abstract R initEmpty();

    public R proceed(Map<K, ? extends Expr> map) {
        if(Expr.useCases) {
            return proceedCases(CaseExpr.pullCases(map));
        } else {
            // ищем первый попавшийся, есть второй алгоритм с непроталкиванием скобок
            for(Map.Entry<K, ? extends Expr> entry : map.entrySet())
                if(!(entry.getValue() instanceof BaseExpr)) {
                    if(entry.getValue() instanceof NullExpr)
                        return initEmpty();
                    IfExpr ifExpr = (IfExpr)entry.getValue();
                    return proceedIf(ifExpr.ifWhere, BaseUtils.replace(map, entry.getKey(), ifExpr.trueExpr), BaseUtils.replace(map, entry.getKey(), ifExpr.falseExpr));
                }

            return proceedBase((Map<K, BaseExpr>) (Map<K, ? extends BaseExpr>) map);
        }
   }
}
