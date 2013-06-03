package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.type.Type;

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
    public Type getSelfType(int i) {
        Expr expr = getExpr(i);
        return expr instanceof ParamExpr ? null : expr.getSelfType();
    }

    @Override
    public String getSource(int i, CompileSource compile) {
        return getExpr(i).getSource(compile);
    }
}
