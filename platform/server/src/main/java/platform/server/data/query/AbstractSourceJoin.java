package platform.server.data.query;

import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.GroupExpr;
import platform.server.data.Table;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.BusinessLogics;
import platform.base.BaseUtils;

import java.util.Map;
import java.util.HashMap;

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
