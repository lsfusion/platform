package platform.server.data.query;

import platform.base.ImmutableObject;
import platform.server.caches.TwinLazy;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class Join<U> extends ImmutableObject {

    public abstract Expr getExpr(U property);
    public abstract Where getWhere();

    public abstract Collection<U> getProperties();

    @TwinLazy
    public Map<U, Expr> getExprs() {
        Map<U, Expr> exprs = new HashMap<U, Expr>();
        for(U property : getProperties())
            exprs.put(property,getExpr(property));
        return exprs;
    }

}

