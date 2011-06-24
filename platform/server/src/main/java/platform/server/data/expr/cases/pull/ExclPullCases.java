package platform.server.data.expr.cases.pull;

import platform.base.BaseUtils;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.cases.ExprCase;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.expr.cases.MapCaseList;
import platform.server.data.query.SourceJoin;
import platform.server.data.where.Where;

import java.util.Map;

public abstract class ExclPullCases<R, K, W extends SourceJoin<W>> {

    protected abstract R proceedBase(W data, Map<K, BaseExpr> map);

    protected abstract R initEmpty();
    protected abstract R add(R op1, R op2);

    public R proceed(final W data, Map<K, ? extends Expr> map) {
        return new PullCases<R, K>() {
            @Override
            protected R proceedCases(MapCaseList<K> cases) {
                R aggregator = initEmpty();
                Where upWhere = Where.FALSE;
                for(MapCase<K> exprCase : cases) {
                    aggregator = add(aggregator, ExclPullCases.this.proceed(data.and(exprCase.where.and(upWhere.not())), exprCase.data));
                    upWhere = upWhere.or(exprCase.where);
                }
                return aggregator;
            }
            @Override
            protected R proceedBase(Map<K, BaseExpr> map) {
                return ExclPullCases.this.proceedBase(data, map);
            }
        }.proceed(map);
    }

}
