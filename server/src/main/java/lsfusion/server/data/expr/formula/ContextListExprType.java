package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.type.Type;

public abstract class ContextListExprType extends ListExprType {

    public ContextListExprType(ImList<? extends Expr> exprs) {
        super(exprs);
    }

    public abstract KeyType getKeyType();

    public Type getType(int i) {
        return exprs.get(i).getType(getKeyType());
    }
}
