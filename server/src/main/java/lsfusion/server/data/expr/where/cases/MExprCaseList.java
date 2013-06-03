package lsfusion.server.data.expr.where.cases;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.CaseExprInterface;
import lsfusion.server.data.where.Where;

public class MExprCaseList extends MCaseList<Expr, Expr, ExprCase> implements CaseExprInterface {

    public MExprCaseList(boolean exclusive) {
        super(exclusive);
    }

    private boolean packExprs = false;
    public MExprCaseList(Where falseWhere, boolean packExprs, boolean exclusive) {
        super(falseWhere, exclusive);
        this.packExprs = packExprs;
    }
    
    private MExprCaseList notExclusive;

    public void add(Where where, Expr expr) {
        if(upWhere.isTrue()) return;

        Where.FollowChange change = new Where.FollowChange();
        where = where.followFalseChange(upWhere, packExprs, change);
        if(where.isFalse()) return;

        if(exclusive && (change.type==Where.FollowType.WIDE || change.type==Where.FollowType.DIFF)) {
            if(notExclusive == null)
                notExclusive = new MExprCaseList(upWhere, packExprs, false);
            notExclusive.add(where, expr);
            return;
        }

        expr = expr.followFalse(Expr.orExprCheck(upWhere, where.not()), packExprs);

        if(exclusive) {
            ExprCaseList exprCases = expr.getCases();
            if(exprCases.size()==0)
                return;
            if(exprCases.size()==1) {
                ExprCase single = exprCases.get(0);
                where = where.and(single.where);
                expr = single.data;
            }                
        }

        ExprCase lastCase;
        if(!exclusive && size()>0 && BaseUtils.hashEquals((lastCase = get(size() - 1)).data, expr)) // если повторяется то просто заor'им
            lastCase.where = lastCase.where.or(where, packExprs);
        else
            add(new ExprCase(where, expr));

        if(!exclusive)
            upWhere = Expr.orExprCheck(upWhere, where);
    }

    private Expr getExclFinal() {
        if(size()==0) return Expr.NULL;

        if(!exclusive) {
            ExprCase lastCase = get(size()-1); // в последнем элементе срезаем null'ы с конца
            lastCase.where = lastCase.where.followFalse(lastCase.data.getWhere().not(), packExprs);
        }

        if(size()==1 && single().where.isTrue())
            return single().data;

        ExprCaseList finalCases;
        if(exclusive)
            finalCases = new ExprCaseList(immutableSet());
        else
            finalCases = new ExprCaseList(immutableList());
        return new CaseExpr(finalCases);
    }

    // возвращает CaseExpr
    public Expr getFinal() {
        Expr exclResult = getExclFinal();
        if(exclusive && notExclusive!=null) {
            notExclusive.add(Where.TRUE, exclResult);
            return notExclusive.getFinal();
        }
        return exclResult;
    }
}
