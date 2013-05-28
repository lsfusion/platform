package platform.server.data.expr.where.ifs;

import platform.base.col.ListFact;
import platform.base.col.interfaces.mutable.MList;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.Case;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.where.Where;

public class MIfCases implements CaseExprInterface {

    MList<Case<Expr>> cases = ListFact.mList();

    public void add(Where where, Expr data) {
        cases.add(new Case<Expr>(where, data));
    }

    public Expr getFinal() {
        Expr result = Expr.NULL;
        for(int i = cases.size()-1;i>=0;i--) {
            Case<Expr> exprCase = cases.get(i);
            result = exprCase.data.ifElse(exprCase.where, result);
        }
        return result;
    }
}
