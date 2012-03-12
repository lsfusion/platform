package platform.server.data.query;

import platform.base.ArrayInstancer;
import platform.base.QuickSet;
import platform.server.caches.AbstractOuterContext;
import platform.server.data.Table;
import platform.server.data.Value;
import platform.server.data.expr.IsClassExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.KeyType;
import platform.server.data.expr.query.*;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.logics.BusinessLogics;

abstract public class AbstractSourceJoin<T extends SourceJoin<T>> extends AbstractOuterContext<T> implements SourceJoin<T> {

    protected static class ToString extends CompileSource  {
        public ToString(QuickSet<Value> values, QuickSet<KeyExpr> keys) {
            super(new KeyType() {
                public Type getKeyType(KeyExpr expr) {
                    return ObjectType.instance;
                }
            }, values.mapString(), BusinessLogics.debugSyntax);
            keySelect.putAll(keys.mapString());
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
        return getSource(new ToString(getOuterValues(), getOuterKeys()));
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
}
