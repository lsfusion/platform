package platform.server.data.expr.where.pull;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.MapCase;
import platform.server.data.expr.where.cases.MapCaseList;
import platform.server.data.query.AndContext;
import platform.server.data.where.Where;

public abstract class ExclPullWheres<R, K, W extends AndContext<W>> {

    protected abstract R proceedBase(W data, ImMap<K, BaseExpr> map);

    protected abstract R initEmpty();
    protected abstract R add(R op1, R op2);

    public R proceed(final W data, ImMap<K, ? extends Expr> map) {
        return new PullWheres<R, K>() {
            protected R proceedCases(MapCaseList<K> cases) {
                R aggregator = initEmpty();
                Where upWhere = Where.FALSE;
                for(MapCase<K> exprCase : cases) {
                    Where caseWhere = exprCase.where;
                    if(!cases.exclusive) {
                        caseWhere = caseWhere.and(upWhere.not());
                        upWhere = upWhere.or(caseWhere);
                    }
                    aggregator = add(aggregator, ExclPullWheres.this.proceed(data.and(caseWhere), exprCase.data));
                }
                return aggregator;
            }

            protected R initEmpty() {
                return ExclPullWheres.this.initEmpty();
            }

            protected R proceedIf(Where ifWhere, ImMap<K, Expr> mapTrue, ImMap<K, Expr> mapFalse) {
                return add(ExclPullWheres.this.proceed(data.and(ifWhere), mapTrue), 
                           ExclPullWheres.this.proceed(data.and(ifWhere.not()), mapFalse));
            }

            protected R proceedBase(ImMap<K, BaseExpr> map) {
                return ExclPullWheres.this.proceedBase(data, map);
            }
        }.proceed(map);
    }

}
