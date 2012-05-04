package platform.server.data.expr.where.pull;

import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.CaseList;
import platform.server.data.expr.where.cases.ExprCaseList;
import platform.server.data.where.Where;

public abstract class ExprPullWheres<K> extends AddPullWheres<K, Expr> {

    @Override
    protected ExprCaseList initCaseList() {
        return new ExprCaseList();
    }

    protected Expr initEmpty() {
        return Expr.NULL;
    }

    protected Expr proceedIf(Where ifWhere, Expr resultTrue, Expr resultFalse) {
        return resultTrue.ifElse(ifWhere, resultFalse);
    }
}
