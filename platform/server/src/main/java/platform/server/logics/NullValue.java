package platform.server.logics;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.where.Where;

public class NullValue extends ObjectValue {

    private NullValue() {
    }
    public static NullValue instance = new NullValue();

    public String getString(SQLSyntax syntax) {
        return SQLSyntax.NULL;
    }

    public boolean isString(SQLSyntax syntax) {
        return true;
    }

    public SourceExpr getExpr() {
        return CaseExpr.NULL;
    }

    public Object getValue() {
        return null;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof NullValue;
    }

    public int hashCode() {
        return 0;
    }

    public Where order(SourceExpr expr, boolean desc, Where orderWhere) {
        Where greater = expr.getWhere();
        if(desc)
            return greater.not().and(orderWhere);
        else
            return greater.or(orderWhere);
    }

}
