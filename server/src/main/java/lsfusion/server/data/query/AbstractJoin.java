package lsfusion.server.data.query;

import lsfusion.base.ImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.cases.CaseJoin;
import lsfusion.server.data.expr.where.ifs.IfJoin;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.where.Where;

public abstract class AbstractJoin<U> extends ImmutableObject implements Join<U> {

    public static <U> Join<U> and(Join<U> join, Where where) {
        if(Expr.useCasesCount <= 1)
            return new CaseJoin<>(where, join);
        else
            return new IfJoin<>(where, join);
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
        return new MapJoin<>(translate, join);
    }
    public Join<U> translateValues(MapValuesTranslate translate) {
        return translateValues(this, translate);
    }
}
