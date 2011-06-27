package platform.server.data.expr.where.pull;

import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.MapCase;
import platform.server.data.expr.where.cases.MapCaseList;
import platform.server.data.query.SourceJoin;
import platform.server.data.where.Where;

import java.util.Map;

public abstract class ExclPullWheres<R, K, W extends SourceJoin<W>> {

    protected abstract R proceedBase(W data, Map<K, BaseExpr> map);

    protected abstract R initEmpty();
    protected abstract R add(R op1, R op2);

    public R proceed(final W data, Map<K, ? extends Expr> map) {
        return new PullWheres<R, K>() {
            @Override
            protected R proceedCases(MapCaseList<K> cases) {
                R aggregator = initEmpty();
                Where upWhere = Where.FALSE;
                for(MapCase<K> exprCase : cases) {
                    aggregator = add(aggregator, ExclPullWheres.this.proceedBase(data.and(exprCase.where.and(upWhere.not())), exprCase.data));
                    upWhere = upWhere.or(exprCase.where);
                }
                return aggregator;
            }

            protected R initEmpty() {
                return ExclPullWheres.this.initEmpty();
            }

            protected R proceedIf(Where ifWhere, Map<K, Expr> mapTrue, Map<K, Expr> mapFalse) {
                return add(ExclPullWheres.this.proceed(data.and(ifWhere), mapTrue), 
                           ExclPullWheres.this.proceed(data.and(ifWhere.not()), mapFalse));
            }

            @Override
            protected R proceedBase(Map<K, BaseExpr> map) {
                return ExclPullWheres.this.proceedBase(data, map);
            }
        }.proceed(map);
    }

}
