package platform.server.data.query.exprs.cases;

import platform.server.data.classes.DataClass;
import platform.server.data.classes.IntegralClass;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.MapContext;
import platform.server.data.types.Type;
import platform.server.where.Where;

public class ExprCaseList extends AddCaseList<AndExpr,ExprCase> {

    public ExprCaseList() {
    }
    public ExprCaseList(AndExpr data) {
        add(Where.TRUE,data);
    }
    public ExprCaseList(Where where, SourceExpr data) {
        if(where.isTrue()) { // если where - true то по сути копирование
            ExprCaseList cases = data.getCases();
            addAll(cases);
            upWhere = cases.upWhere;
            nullWhere = cases.nullWhere;
        } else
            add(where, data);
    }
    public ExprCaseList(Where where, SourceExpr exprTrue, SourceExpr exprFalse) {
        add(where,exprTrue);
        add(Where.TRUE,exprFalse);
    }
    public ExprCaseList(Where falseWhere) {
        super(falseWhere);
    }

    // возвращает CaseExpr
    public SourceExpr getExpr() {
        return new CaseExpr(this);
    }

    Where nullWhere = Where.FALSE;

    public void add(Where where,AndExpr expr) {
        where = where.and(nullWhere.not()).followFalse(upWhere);
        if(where.isFalse()) return;

        Where falseWhere = upWhere.or(where.not());
        if((expr=expr.andFollowFalse(falseWhere))==null) // если заведомо null
            nullWhere = nullWhere.or(where);
        else {
            ExprCase lastCase = size()>0?get(size()-1):null;
            if(lastCase!=null && lastCase.data.equals(expr)) // если повторяется то просто заor'им
                lastCase.where = lastCase.where.or(where);
            else
                add(new ExprCase(where, expr));
            upWhere = upWhere.or(where);
        }
    }

    // добавляет case
    public void add(Where where,SourceExpr expr) {
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

    public boolean equals(ExprCaseList cases,MapContext mapContext) {
        if(!(size()==cases.size())) return false;

        for(int i=0;i<size();i++)
            if(!get(i).equals(cases.get(i),mapContext))
                return false;
        return true;
    }

    public int hash() {
        int hash = 0;
        for(ExprCase exprCase : this)
            hash = 31*hash + exprCase.hash();
        return hash;
    }
}
