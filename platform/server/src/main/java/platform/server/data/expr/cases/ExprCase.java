package platform.server.data.expr.cases;

import platform.server.caches.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.where.Where;

public class ExprCase extends Case<BaseExpr> {

    public ExprCase(Where where, BaseExpr expr) {
        super(where,expr);
    }

    public ExprCase(BaseExpr expr) {
        super(Where.TRUE,expr);
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
        return where.hashContext(hashContext)*31+ data.hashContext(hashContext);
    }
}
