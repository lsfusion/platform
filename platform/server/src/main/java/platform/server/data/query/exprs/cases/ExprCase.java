package platform.server.data.query.exprs.cases;

import platform.server.data.query.HashContext;
import platform.server.data.query.exprs.AndExpr;
import platform.server.where.Where;

public class ExprCase extends Case<AndExpr> {

    public ExprCase(Where iWhere, AndExpr iExpr) {
        super(iWhere,iExpr);
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
