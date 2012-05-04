package platform.server.data.expr.where.ifs;

import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.expr.where.Case;
import platform.server.data.where.Where;
import platform.server.data.where.OrWhere;

import java.util.List;
import java.util.ArrayList;

public class IfCases implements CaseExprInterface {

    List<Case<Expr>> cases = new ArrayList<Case<Expr>>();

    public void add(Where where, Expr data) {
        cases.add(new Case<Expr>(where, data));
    }

    public Where getUpWhere() {
        Where result = Where.FALSE;
        for(Case<Expr> caseExpr : cases)
            result = result.or(caseExpr.where);
        return result;
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
