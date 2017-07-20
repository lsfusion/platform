package lsfusion.server.data.expr.where.cases;

import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.Case;
import lsfusion.server.data.where.Where;

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

    public int hashOuter(HashContext hashContext) {
        return where.hashOuter(hashContext)*31+ data.hashOuter(hashContext);
    }

    public long getComplexity(boolean outer) {
        return where.getComplexity(outer) + data.getComplexity(outer);
    }
}
