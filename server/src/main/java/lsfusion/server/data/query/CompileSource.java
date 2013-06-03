package lsfusion.server.data.query;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.ParseValue;
import lsfusion.server.data.Table;
import lsfusion.server.data.expr.IsClassExpr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.expr.query.QueryExpr;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.where.Where;

// класс нисколько не immutable
public abstract class CompileSource {

    private final GetValue<String, SourceJoin> getSource = new GetValue<String, SourceJoin>() {
        public String getMapValue(SourceJoin value) {
            return value.getSource(CompileSource.this);
        }};
    public <V extends SourceJoin> GetValue<String, V> GETSOURCE() {
        return (GetValue<String, V>)getSource;
    }

    public final ImRevMap<ParseValue,String> params;
    public final SQLSyntax syntax;
    public final ExecuteEnvironment env;

    public final KeyType keyType;
    public final Where fullWhere;

    protected CompileSource(KeyType keyType, Where fullWhere, ImRevMap<ParseValue, String> params, SQLSyntax syntax, ExecuteEnvironment env) {
        this.keyType = keyType;
        this.fullWhere = fullWhere;
        this.params = params;
        this.syntax = syntax;
        this.env = env;
    }

    public abstract String getSource(KeyExpr key);
    public abstract String getSource(Table.Join.Expr expr);
    public abstract String getSource(Table.Join.IsIn where);
    public String getNullSource(QueryExpr queryExpr, boolean notNull) {
        return getSource(queryExpr) + " IS" + (notNull?" NOT":"") + " NULL";
    }
    public abstract String getSource(QueryExpr queryExpr);

    public abstract String getSource(IsClassExpr classExpr);
    
    // для binarywhere
    public boolean means(Where where) {
        return fullWhere.means(where);
    }
    
}
