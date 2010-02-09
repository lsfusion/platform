package platform.server.data.query;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;

import java.util.Map;
import java.util.Collection;

public abstract class SourceEnumerator {

    public void add(KeyExpr keyExpr) {
    }
    public void add(ValueExpr valueExpr) {        
    }

    public <U> void fill(Map<U, ? extends SourceJoin> exprs) {
        fill(exprs.values());
    }
    public void fill(Collection<? extends SourceJoin> exprs) {
        for(SourceJoin expr : exprs)
            expr.enumerate(this);
    }
}
