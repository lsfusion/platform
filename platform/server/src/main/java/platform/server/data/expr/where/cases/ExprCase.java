package platform.server.data.expr.where.cases;

import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.where.Case;
import platform.server.data.where.Where;

public class ExprCase extends Case<BaseExpr> {

    public ExprCase(Where where, BaseExpr expr) {
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

    public long getComplexity() {
        return where.getComplexity() + data.getComplexity();
    }
}
