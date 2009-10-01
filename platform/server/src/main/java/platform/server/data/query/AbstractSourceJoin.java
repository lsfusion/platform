package platform.server.data.query;

import platform.base.BaseUtils;
import platform.server.data.Table;
import platform.server.data.query.exprs.GroupExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.logics.BusinessLogics;

abstract public class AbstractSourceJoin implements SourceJoin {

    public abstract int hashContext(HashContext hashContext);

    boolean hashCoded = false;
    int hashCode;
    public int hashCode() {
        if(!hashCoded) {
            hashCode = hashContext(new HashContext() {
                public int hash(KeyExpr expr) {
                    return expr.hashCode();
                }
                public int hash(ValueExpr expr) {
                    return expr.hashCode();
                }
            });
            hashCoded = true;
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj!=null && getClass() == obj.getClass() && twins((AbstractSourceJoin) obj);
    }

    public abstract boolean twins(AbstractSourceJoin obj);

    protected static class ToString extends CompileSource  {
        public ToString(Context context) {
            super(BaseUtils.mapString(context.values), BusinessLogics.debugSyntax);
            keySelect.putAll(BaseUtils.mapString(context.keys));
        }

        public String getSource(Table.Join.Expr expr) {
            return expr.toString();
        }

        public String getSource(Table.Join.IsIn where) {
            return where.toString();
        }

        public String getSource(GroupExpr groupExpr) {
            return groupExpr.toString();
        }
    }

    @Override
    public String toString() {
        Context context = new Context();
        fillContext(context);
        return getSource(new ToString(context));
    }
}
