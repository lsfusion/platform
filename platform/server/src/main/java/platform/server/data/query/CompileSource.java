package platform.server.data.query;

import platform.server.data.Table;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.OrderExpr;
import platform.server.data.sql.SQLSyntax;

import java.util.HashMap;
import java.util.Map;

// класс нисколько не immutable
public abstract class CompileSource {

    public final Map<ValueExpr,String> params;
    public final Map<KeyExpr,String> keySelect = new HashMap<KeyExpr, String>();
    public final SQLSyntax syntax;

    protected CompileSource(Map<ValueExpr, String> params, SQLSyntax syntax) {
        this.params = params;
        this.syntax = syntax;
    }

    public abstract String getSource(Table.Join.Expr expr);
    public abstract String getSource(Table.Join.IsIn where);
    public abstract String getSource(GroupExpr groupExpr);
    public abstract String getSource(OrderExpr orderExpr);
}
