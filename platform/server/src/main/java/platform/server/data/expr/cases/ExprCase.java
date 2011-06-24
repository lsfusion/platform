package platform.server.data.expr.cases;

import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;

public class ExprCase extends Case<Expr> {

    public ExprCase(Where where, Expr expr) {
        super(where,expr);
    }

    public String toString() {
        return where.toString() + "-" + data.toString();
    }

    public boolean equals(Object obj) {
        return this==obj || obj instanceof ExprCase && where.equals(((ExprCase)obj).where) && data.equals(((ExprCase)obj).data);
    }

    public int hashCode() {
        return where.hashCode()*31+data.hashCode();
    }

    int hashContext(HashContext hashContext) {
        return where.hashOuter(hashContext)*31+ data.hashOuter(hashContext);
    }

    public long getComplexity() {
        return where.getComplexity() + data.getComplexity();
    }
}
