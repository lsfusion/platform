package lsfusion.server.data.query.compile;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.query.MStaticExecuteEnvironment;
import lsfusion.server.data.table.Table;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.query.QueryExpr;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.where.Where;

// класс нисколько не immutable
public abstract class CompileSource {

    private final GetValue<String, Expr> getExprSource = new GetValue<String, Expr>() {
        public String getMapValue(Expr value) {
            return value.getSource(CompileSource.this);
        }};
    public GetValue<String, Expr> GETEXPRSOURCE() {
        return getExprSource;
    }
    private final GetValue<String, Where> getWhereSource = new GetValue<String, Where>() {
        public String getMapValue(Where value) {
            return value.getSource(CompileSource.this);
        }};
    public GetValue<String, Where> GETWHERESOURCE() {
        return getWhereSource;
    }

    public final ImRevMap<ParseValue,String> params;
    public final SQLSyntax syntax;
    public final MStaticExecuteEnvironment env;

    public final KeyType keyType;
    public final Where fullWhere;

    protected CompileSource(KeyType keyType, Where fullWhere, ImRevMap<ParseValue, String> params, SQLSyntax syntax, MStaticExecuteEnvironment env) {
        this.keyType = keyType;
        this.fullWhere = fullWhere;
        this.params = params;
        this.syntax = syntax;
        this.env = env;
    }

    public abstract String getSource(KeyExpr key);
    public abstract String getSource(Table.Join.Expr expr);
    public abstract String getSource(Table.Join.IsIn where);
    public abstract String getSource(QueryExpr queryExpr, boolean needValue);

    public String getNullSource(InnerExpr innerExpr, String defaultSource) {
        return defaultSource;
    }

    public abstract String getSource(IsClassExpr classExpr, boolean needValue);
    
    // для binarywhere
    public boolean means(Where where) {
        return fullWhere.means(where);
    }
    
}
