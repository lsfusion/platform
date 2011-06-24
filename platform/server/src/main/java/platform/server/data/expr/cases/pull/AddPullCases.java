package platform.server.data.expr.cases.pull;

import platform.base.BaseUtils;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.cases.CaseList;
import platform.server.data.expr.cases.ExprCase;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.expr.cases.MapCaseList;

import java.util.Map;

public abstract class AddPullCases<K,R> extends PullCases<R, K> {

    protected abstract CaseList<R, ?> initAggregator();

    @Override
    protected R proceedCases(MapCaseList<K> cases) {
        CaseList<R, ?> aggregator = initAggregator();
        for(MapCase<K> exprCase : cases)
            aggregator.add(exprCase.where, proceed(exprCase.data));
        return aggregator.getFinal();
    }
}
