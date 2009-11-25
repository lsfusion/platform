package platform.server.logics;

import platform.server.data.expr.Expr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.where.Where;

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

    public Expr getExpr() {
        return Expr.NULL;
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

    public Where order(Expr expr, boolean desc, Where orderWhere) {
        Where greater = expr.getWhere();
        if(desc)
            return greater.not().and(orderWhere);
        else
            return greater.or(orderWhere);
    }

}
