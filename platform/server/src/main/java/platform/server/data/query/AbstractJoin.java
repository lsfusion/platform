package platform.server.data.query;

import platform.base.ImmutableObject;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.CaseJoin;
import platform.server.data.expr.where.ifs.IfJoin;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;

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

    public static <U> ImMap<U, Expr> getExprs(final Join<U> join) {
        return join.getProperties().mapValues(new GetValue<Expr, U>() {
            public Expr getMapValue(U value) {
                return join.getExpr(value);
            }});
    }

    @IdentityLazy
    public ImMap<U, Expr> getExprs() {
        return getExprs(this);
    }

    public static <U> Join<U> translateValues(Join<U> join, MapValuesTranslate translate) {
        return new MapJoin<U>(translate, join);
    }
    public Join<U> translateValues(MapValuesTranslate translate) {
        return translateValues(this, translate);
    }
}
