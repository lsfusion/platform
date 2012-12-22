package platform.server.data.query;

import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.data.Table;
import platform.server.data.Value;
import platform.server.data.expr.IsClassExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.query.QueryExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.where.Where;

// класс нисколько не immutable
public abstract class CompileSource {

    private final GetValue<String, SourceJoin> getSource = new GetValue<String, SourceJoin>() {
        public String getMapValue(SourceJoin value) {
            return value.getSource(CompileSource.this);
        }};
    public <V extends SourceJoin> GetValue<String, V> GETSOURCE() {
        return (GetValue<String, V>)getSource;
    }

    public final ImRevMap<Value,String> params;
    public final SQLSyntax syntax;

    public final KeyType keyType;
    public final Where fullWhere;

    protected CompileSource(KeyType keyType, Where fullWhere, ImRevMap<Value, String> params, SQLSyntax syntax) {
        this.keyType = keyType;
        this.fullWhere = fullWhere;
        this.params = params;
        this.syntax = syntax;
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
