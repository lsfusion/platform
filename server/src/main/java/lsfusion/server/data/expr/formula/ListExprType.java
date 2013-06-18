package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.type.Type;

public abstract class ListExprType implements ExprType {

    protected final ImList<? extends Expr> exprs;

    public ListExprType(ImList<? extends Expr> exprs) {
        this.exprs = exprs;
    }

    public int getExprCount() {
        return exprs.size();
    }

    public boolean isParam(int i) {
        return exprs.get(i) instanceof ParamExpr;
    }
}
