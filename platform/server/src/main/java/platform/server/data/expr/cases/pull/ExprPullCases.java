package platform.server.data.expr.cases.pull;

import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.cases.CaseList;
import platform.server.data.expr.cases.ExprCaseList;

import java.awt.image.Kernel;
import java.util.Map;

public abstract class ExprPullCases<K> extends AddPullCases<K, Expr> {

    @Override
    protected CaseList<Expr, ?> initAggregator() {
        return new ExprCaseList();
    }
}
