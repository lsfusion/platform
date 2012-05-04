package platform.server.data.query;

import platform.base.ImmutableObject;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.CaseJoin;
import platform.server.data.expr.where.ifs.IfJoin;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractJoin<U> extends ImmutableObject implements Join<U> {

    public static <U> Join<U> and(Join<U> join, Where where) {
        if(Expr.useCases)
            return new CaseJoin<U>(where, join);
        else
            return new IfJoin<U>(where, join);
    }

    public Join<U> and(Where where) {
        return and(this, where);
    }

    public static <U> Map<U, Expr> getExprs(Join<U> join) {
        Map<U, Expr> exprs = new HashMap<U, Expr>();
        for(U property : join.getProperties())
            exprs.put(property,join.getExpr(property));
        return exprs;
    }

    @IdentityLazy
    public Map<U, Expr> getExprs() {
        return getExprs(this);
    }

    public static <U> Join<U> translateValues(Join<U> join, MapValuesTranslate translate) {
        return new MapJoin<U>(translate, join);
    }
    public Join<U> translateValues(MapValuesTranslate translate) {
        return translateValues(this, translate);
    }

    @Override
    public int hashCode() {
        throw new RuntimeException("should not be");
    }

    @Override
    public boolean equals(Object obj) {
        throw new RuntimeException("should not be");
    }
}
