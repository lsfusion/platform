package platform.server.data.query;

import platform.server.data.Table;
import platform.server.data.Value;
import platform.server.data.expr.IsClassExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.query.*;
import platform.server.data.sql.SQLSyntax;

import java.util.HashMap;
import java.util.Map;

// класс нисколько не immutable
public abstract class CompileSource {

    public final Map<Value,String> params;
    public final Map<KeyExpr,String> keySelect = new HashMap<KeyExpr, String>();
    public final SQLSyntax syntax;

    public final KeyType keyType;

    protected CompileSource(KeyType keyType, Map<Value, String> params, SQLSyntax syntax) {
        this.keyType = keyType;
        this.params = params;
        this.syntax = syntax;
    }

    public abstract String getSource(Table.Join.Expr expr);
    public abstract String getSource(Table.Join.IsIn where);
    public String getNullSource(QueryExpr queryExpr, boolean notNull) {
        return getSource(queryExpr) + " IS" + (notNull?" NOT":"") + " NULL";
    }
    public abstract String getSource(QueryExpr queryExpr);

    public abstract String getSource(IsClassExpr classExpr);
}
