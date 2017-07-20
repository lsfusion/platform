package lsfusion.server.data.expr.where.ifs;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.Case;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.where.Where;

public class MIfCases implements CaseExprInterface {

    MList<Case<Expr>> cases = ListFact.mList();

    public void add(Where where, Expr data) {
        cases.add(new Case<>(where, data));
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
