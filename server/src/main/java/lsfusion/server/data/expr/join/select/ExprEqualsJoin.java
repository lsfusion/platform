package lsfusion.server.data.expr.join.select;

import lsfusion.server.data.expr.BaseExpr;

public class ExprEqualsJoin extends ExprCompareJoin<BaseExpr, ExprEqualsJoin> {

    public ExprEqualsJoin(BaseExpr expr1, BaseExpr expr2) {
        super(expr1, expr2);
    }

    @Override
    protected ExprEqualsJoin createThis(BaseExpr expr1, BaseExpr expr2) {
        return new ExprEqualsJoin(expr1, expr2);
    }
}
