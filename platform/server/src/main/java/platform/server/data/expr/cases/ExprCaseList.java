package platform.server.data.expr.cases;

import platform.base.BaseUtils;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.VariableExprSet;
import platform.server.data.where.Where;

public class ExprCaseList extends CaseList<Expr,ExprCase> {

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
    public Expr getFinal() {
        if(size()==0) return Expr.NULL;

        ExprCase lastCase = get(size()-1); // в последнем элементе срезаем null'ы с конца
        lastCase.where = lastCase.where.followFalse(lastCase.data.getWhere().not(), packExprs);

        if(size()==1 && lastCase.where.isTrue())
            return lastCase.data;
        return new CaseExpr(this);
    }

    private Where nullWhere = Where.FALSE;

    private final boolean mergeCases = false;

    private void addBase(Where where, Expr expr) {
        expr = expr.followFalse(getUpWhere().or(where.not()), packExprs);
        if(mergeCases && !(expr instanceof BaseExpr)) {// на самом деле если не packExprs либо BaseExpr либо NULL
            add(where, expr);
            return;
        }

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
        if(mergeCases) {
            Where exprNullWhere = Where.FALSE;
            for(ExprCase addCase : expr.getCases()) {
                addBase(where.and(addCase.where),addCase.data);
                exprNullWhere = exprNullWhere.or(addCase.where);
            }
            nullWhere = nullWhere.or(where.and(exprNullWhere.not()));
        } else
            addBase(where, expr);
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

    public long getComplexity() {
        long complexity = 0;
        for(ExprCase exprCase : this)
            complexity += exprCase.getComplexity();
        return complexity;
    }

    public VariableExprSet getExprFollows() {
        if(size()==0) return new VariableExprSet();
        VariableExprSet[] follows = new VariableExprSet[size()] ; int num = 0;
        for(ExprCase expr : this)
            follows[num++] = expr.data.getExprFollows();
        return new VariableExprSet(follows);
    }
}
