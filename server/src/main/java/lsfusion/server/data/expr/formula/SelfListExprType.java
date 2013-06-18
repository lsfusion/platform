package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.type.Type;

public class SelfListExprType extends ListExprType {

    public SelfListExprType(ImList<? extends Expr> exprs) {
        super(exprs);
    }

    public Type getType(int i) {
        if(isParam(i))
            return null;
        return exprs.get(i).getSelfType();
    }
}
