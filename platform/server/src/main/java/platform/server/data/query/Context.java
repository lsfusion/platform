package platform.server.data.query;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Context {

    public Context() {
        keys = new HashSet<KeyExpr>();
        values = new HashSet<ValueExpr>();
    }

    public Context(Set<KeyExpr> keys, Set<ValueExpr> values) {
        this.keys = keys;
        this.values = values;
    }

    public final Set<KeyExpr> keys;
    public final Set<ValueExpr> values;

    public <U> void fill(Map<U, ? extends SourceJoin> exprs) {
        fill(exprs.values());
    }
    public void fill(Collection<? extends SourceJoin> exprs) {
        for(SourceJoin expr : exprs)
            expr.fillContext(this);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Context && keys.equals(((Context) o).keys) && values.equals(((Context) o).values);
    }

    @Override
    public int hashCode() {
        return 31 * keys.hashCode() + values.hashCode();
    }
}
