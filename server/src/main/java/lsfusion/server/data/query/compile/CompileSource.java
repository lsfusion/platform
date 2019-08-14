package lsfusion.server.data.query.compile;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.IsClassExpr;
import lsfusion.server.data.expr.inner.InnerExpr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.expr.query.QueryExpr;
import lsfusion.server.data.query.exec.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.table.Table;
import lsfusion.server.data.where.Where;

import java.util.function.Function;

// класс нисколько не immutable
public abstract class CompileSource {

    private final Function<Expr, String> getExprSource = value -> value.getSource(CompileSource.this);
    public Function<Expr, String> GETEXPRSOURCE() {
        return getExprSource;
    }
    private final Function<Where, String> getWhereSource = value -> value.getSource(CompileSource.this);
    public Function<Where, String> GETWHERESOURCE() {
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
