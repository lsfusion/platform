package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.server.caches.Lazy;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.PropertyField;
import platform.server.where.Where;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Immutable
public abstract class Join<U> {

    public abstract SourceExpr getExpr(U property);
    public abstract Where<?> getWhere();

    public abstract Collection<U> getProperties();

    @Lazy
    public Map<U, SourceExpr> getExprs() {
        Map<U,SourceExpr> exprs = new HashMap<U,SourceExpr>();
        for(U property : getProperties())
            exprs.put(property,getExpr(property));
        return exprs;
    }

}

