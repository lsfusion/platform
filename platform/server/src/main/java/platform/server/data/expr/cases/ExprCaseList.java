package platform.server.data.expr.cases;

import platform.base.BaseUtils;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;

public class ExprCaseList extends AddCaseList<BaseExpr,ExprCase> {

    public ExprCaseList() {
    }
    public ExprCaseList(BaseExpr data) {
        add(new ExprCase(Where.TRUE,data));
        upWhere = Where.TRUE;
    }
    public ExprCaseList(Where where, Expr data) {
        add(where, data);
    }
    public ExprCaseList(Where where, Expr exprTrue, Expr exprFalse) {
        add(where,exprTrue);
        add(Where.TRUE,exprFalse);
    }

    private boolean packExprs = false;
    public ExprCaseList(Where falseWhere, ExprCaseList followCases, boolean packExprs) {
        super(falseWhere);
        
        this.packExprs = packExprs;

        for(ExprCase exprCase : followCases)
            add(exprCase.where,exprCase.data);
    }

    // возвращает CaseExpr
    public Expr getExpr() {
        if(size()>0) {
            ExprCase lastCase = get(size()-1); // в последнем элементе срезаем null'ы с конца
            lastCase.where = lastCase.where.followFalse(lastCase.data.getWhere().not(), packExprs);
        }
        if(size()==1 && get(0).where.isTrue())
            return get(0).data;
        return new CaseExpr(this);
    }

    private Where nullWhere = Where.FALSE;

    private void add(Where where, BaseExpr expr) {
        Expr followExpr = expr.followFalse(getUpWhere().or(where.not()), packExprs);
        if(!(followExpr instanceof BaseExpr)) // на самом деле если не packExprs либо BaseExpr либо NULL
            add(where, followExpr);

        where = where.and(nullWhere.not()).followFalse(upWhere, packExprs);
        if(where.isFalse()) return;

        ExprCase lastCase = size()>0?get(size()-1):null;
        if(lastCase!=null && BaseUtils.hashEquals(lastCase.data,expr)) // если повторяется то просто заor'им
            lastCase.where = lastCase.where.or(where, packExprs);
        else
            add(new ExprCase(where, expr));

        upWhere = upWhere.or(where);
    }

    // добавляет case
    public void add(Where where, Expr expr) {
        // нужно еше насчитать exprNullWhere и захерачить его в общий nullWhere
        Where exprNullWhere = Where.FALSE;
        for(ExprCase addCase : expr.getCases()) {
            add(where.and(addCase.where),addCase.data);
            exprNullWhere = exprNullWhere.or(addCase.where);
        }

        nullWhere = nullWhere.or(where.and(exprNullWhere.not()));
    }

    // узнает использованный where
    public Where getUpWhere() {
        return upWhere.or(nullWhere);
    }

    int hashContext(HashContext hashContext) {
        int hash = 0;
        for(ExprCase exprCase : this)
            hash = 31*hash + exprCase.hashContext(hashContext);
        return hash;
    }
}
