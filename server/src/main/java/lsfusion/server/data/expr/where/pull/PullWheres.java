package lsfusion.server.data.expr.where.pull;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.cases.*;
import lsfusion.server.data.expr.where.ifs.IfExpr;
import lsfusion.server.data.expr.where.ifs.NullExpr;
import lsfusion.server.data.where.Where;

public abstract class PullWheres<R, K> {

    protected abstract R proceedCases(MapCaseList<K> cases);
    protected abstract R proceedIf(Where ifWhere, ImMap<K, Expr> mapTrue, ImMap<K, Expr> mapFalse);
    protected abstract R proceedBase(ImMap<K, BaseExpr> map);

    protected abstract R initEmpty();

    public R proceed(final ImMap<K, ? extends Expr> map) {
        // ищем первый попавшийся, есть второй алгоритм с непроталкиванием скобок
        for(int i=0,size=map.size();i<size;i++) {
            final K key = map.getKey(i);
            Expr expr = map.getValue(i);
            if(!(expr instanceof BaseExpr)) {
                if(expr instanceof NullExpr)
                    return initEmpty();
                if(expr instanceof CaseExpr) {
                    ExprCaseList cases = expr.getCases();
                    GetValue<MapCase<K>, ExprCase> mapCases = new GetValue<MapCase<K>, ExprCase>() {
                        public MapCase<K> getMapValue(ExprCase value) {
                            return new MapCase<K>(value.where, ((ImMap<K, Expr>) map).replaceValue(key, value.data));
                        }};
                    return proceedCases(cases.mapValues(mapCases));
                }
                IfExpr ifExpr = (IfExpr)expr;
                return proceedIf(ifExpr.ifWhere,
                                    ((ImMap<K, Expr>)map).replaceValue(key, ifExpr.trueExpr),
                                    ((ImMap<K, Expr>)map).replaceValue(key, ifExpr.falseExpr));
            }
        }

        return proceedBase((ImMap<K, BaseExpr>) (ImMap<K, ? extends BaseExpr>) map);
   }
}
