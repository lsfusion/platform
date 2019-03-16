package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyType;

public abstract class ListExprType implements ExprType {

    protected final ImList<? extends Expr> exprs;

    public ListExprType(ImList<? extends Expr> exprs) {
        this.exprs = exprs;
    }

    public int getExprCount() {
        return exprs.size();
    }
    
    public static ListExprType create(final KeyType keyType, ImList<? extends Expr> exprs) {
        return keyType == null ? new SelfListExprType(exprs) : new ContextListExprType(exprs) {
            public KeyType getKeyType() {
                return keyType;
            }
        };
    }
}
