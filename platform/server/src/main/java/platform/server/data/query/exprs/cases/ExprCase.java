package platform.server.data.query.exprs.cases;

import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.MapContext;
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

    // для кэша
    boolean equals(ExprCase aCase, MapContext mapJoins) {
        return where.equals(aCase.where, mapJoins) && data.equals(aCase.data, mapJoins);
    }

    int hash() {
        return where.hash()*31+ data.hash();
    }
}
