package platform.server.data.query;

import platform.base.ImmutableObject;
import platform.server.caches.TwinLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.CaseJoin;
import platform.server.data.expr.where.ifs.IfJoin;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class Join<U> extends ImmutableObject {

    public Join<U> and(Where where) {
        if(Expr.useCases)
            return new CaseJoin<U>(where, this);
        else
            return new IfJoin<U>(where, this);
    }

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

