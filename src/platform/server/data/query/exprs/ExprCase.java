package platform.server.data.query.exprs;

import platform.server.data.query.wheres.JoinWhere;
import platform.server.where.Where;

import java.util.Map;

public class ExprCase extends Case<SourceExpr> {

    public ExprCase(Where iWhere, AndExpr iExpr) {
        this(iWhere,(SourceExpr)iExpr);
    }

    // дублируем чтобы различать
    ExprCase(Where iWhere, SourceExpr iExpr) {
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
    boolean equals(ExprCase Case, Map<ObjectExpr, ObjectExpr> MapExprs, Map<JoinWhere, JoinWhere> MapWheres) {
        return where.equals(Case.where, MapExprs, MapWheres) && data.equals(Case.data, MapExprs, MapWheres);
    }

    int hash() {
        return where.hash()*31+ data.hash();
    }
}
