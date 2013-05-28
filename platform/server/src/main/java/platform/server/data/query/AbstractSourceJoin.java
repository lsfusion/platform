package platform.server.data.query;

import platform.base.ArrayInstancer;
import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.Settings;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.ParamExpr;
import platform.server.data.ParseValue;
import platform.server.data.Table;
import platform.server.data.Value;
import platform.server.data.expr.IsClassExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.query.QueryExpr;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

abstract public class AbstractSourceJoin<T extends SourceJoin<T>> extends AbstractOuterContext<T> implements SourceJoin<T> {

    protected static class ToString extends CompileSource  {
        public ToString(ImSet<Value> values) {
            super(new KeyType() {
                public Type getKeyType(ParamExpr expr) {
                    return ObjectType.instance;
                }
            }, Where.FALSE, BaseUtils.<ImSet<ParseValue>>immutableCast(values).mapRevValues(new GetValue<String, ParseValue>() {
                public String getMapValue(ParseValue value) {
                    return value.toString();
                }}), PostgreDataAdapter.debugSyntax, new ExecuteEnvironment());
        }

        public String getSource(KeyExpr expr) {
            return expr.toString();
        }

        public String getSource(Table.Join.Expr expr) {
            return expr.toString();
        }

        public String getSource(Table.Join.IsIn where) {
            return where.toString();
        }

        public String getSource(QueryExpr queryExpr) {
            return queryExpr.toString();
        }

        public String getSource(IsClassExpr classExpr) {
            return "class(" + classExpr.expr.getSource(this) + ")";
        }
    }

    @Override
    public String toString() {
        return getSource(new ToString(getOuterValues()));
    }

    public final static ArrayInstancer<SourceJoin> instancer = new ArrayInstancer<SourceJoin>() {
        public SourceJoin[] newArray(int size) {
            return new SourceJoin[size];
        }
    };

    // упрощаем зная where == false
    public abstract T followFalse(Where falseWhere, boolean pack);

    public T calculatePack() {
        return followFalse(Where.FALSE, true);
    }

    public boolean needMaterialize() {
        if(getComplexity(false) > Settings.get().getLimitMaterializeComplexity())
            return true;

        return false;
    }
}
