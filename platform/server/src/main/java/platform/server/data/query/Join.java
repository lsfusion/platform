package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.server.caches.Lazy;
import platform.server.caches.TwinLazy;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.where.Where;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class Join<U> {

    public abstract SourceExpr getExpr(U property);
    public abstract Where<?> getWhere();

    public abstract Collection<U> getProperties();

    @TwinLazy
    public Map<U, SourceExpr> getExprs() {
        Map<U,SourceExpr> exprs = new HashMap<U,SourceExpr>();
        for(U property : getProperties())
            exprs.put(property,getExpr(property));
        return exprs;
    }

}

