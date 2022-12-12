package lsfusion.server.data.expr.where.pull;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.cases.*;
import lsfusion.server.data.expr.where.ifs.IfExpr;
import lsfusion.server.data.expr.where.ifs.NullExpr;
import lsfusion.server.data.where.Where;

import java.util.function.Function;

public abstract class PullWheres<R, K> {

    protected abstract R proceedCases(MapCaseList<K> cases, ImMap<K, Expr> nullCase);
    protected abstract R proceedIf(Where ifWhere, ImMap<K, Expr> mapTrue, ImMap<K, Expr> mapFalse);

    protected abstract R proceedBase(ImMap<K, BaseExpr> map);
    protected abstract R initEmpty(); // assert !supportNulls

    protected boolean supportNulls() {
        return false;
    }
    protected R proceedNullBase(ImMap<K, ? extends Expr> map) { // baseExpr and NullExpr, Empty cases
        return proceedBase((ImMap<K, BaseExpr>)map);
    }

    public R proceed(final ImMap<K, ? extends Expr> map) {
        boolean supportNulls = supportNulls();
        for(int i=0,size=map.size();i<size;i++) {
            final K key = map.getKey(i);
            Expr expr = map.getValue(i);
            if(!(expr instanceof BaseExpr || (supportNulls && expr.isNull()))) {
                if(expr instanceof NullExpr)
                    return initEmpty();
                if(expr instanceof CaseExpr) {
                    ExprCaseList cases = expr.getCases();
                    Function<ExprCase, MapCase<K>> mapCases = value -> new MapCase<>(value.where, ((ImMap<K, Expr>) map).replaceValue(key, value.data));
                    return proceedCases(cases.mapValues(mapCases), supportNulls ? ((ImMap<K, Expr>) map).replaceValue(key, Expr.NULL()) : null);
                }
                IfExpr ifExpr = (IfExpr)expr;
                return proceedIf(ifExpr.ifWhere,
                                    ((ImMap<K, Expr>)map).replaceValue(key, ifExpr.trueExpr),
                                    ((ImMap<K, Expr>)map).replaceValue(key, ifExpr.falseExpr));
            }
        }

        return proceedNullBase(map);
   }
}
