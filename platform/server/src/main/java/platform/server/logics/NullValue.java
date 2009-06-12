package platform.server.logics;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;

public class NullValue extends ObjectValue {

    public NullValue() {
    }

    public String getString(SQLSyntax syntax) {
        return SQLSyntax.NULL;
    }

    public boolean isString(SQLSyntax syntax) {
        return true;
    }

    public SourceExpr getExpr() {
        return new CaseExpr();
    }

    public boolean equals(Object o) {
        return this==o || o instanceof NullValue;
    }

    public int hashCode() {
        return 0;
    }
}
