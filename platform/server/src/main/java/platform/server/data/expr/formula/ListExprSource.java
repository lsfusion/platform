package platform.server.data.expr.formula;

import platform.base.col.interfaces.immutable.ImList;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyType;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public class ListExprSource implements ExprSource {
    private final ImList<? extends Expr> exprs;

    public ListExprSource(ImList<? extends Expr> exprs) {
        this.exprs = exprs;
    }

    @Override
    public int getExprCount() {
        return exprs.size();
    }

    @Override
    public Expr getExpr(int i) {
        return exprs.get(i);
    }

    @Override
    public Type getType(int i, KeyType keyType) {
        return getExpr(i).getType(keyType);
    }

    @Override
    public String getSource(int i, CompileSource compile) {
        return getExpr(i).getSource(compile);
    }
}
