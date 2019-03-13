package lsfusion.server.session;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.caches.AbstractInnerContext;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.where.Where;

import static lsfusion.base.BaseUtils.hashEquals;

public class PropertySet<T> extends AbstractInnerContext<PropertySet<T>> {
    private final ImRevMap<T,KeyExpr> mapKeys;
    private final Where where;

    public PropertySet(ImRevMap<T, KeyExpr> mapKeys, Where where) {
        this.mapKeys = mapKeys;
        this.where = where;
    }

    private Where getFullWhere() {
        return where;
    }

    @IdentityInstanceLazy
    private Query<T,String> getQuery() {
        QueryBuilder<T, String> query = new QueryBuilder<>(mapKeys, getFullWhere());
        return query.getQuery();
    }

    // временный пока фикс из-за ошибки postgre при Partition запросах, когда при наличии SUM ORDER BY expr начинает GroupAggregate крутить в цикле
    // materialize subqueries могут решить эту проблему
    public boolean needMaterialize() {
        if(where.needMaterialize())
            return true;
        return false;
    }

    public PropertySet<T> and(Where andWhere) {
        if(andWhere.isTrue())
            return this;

        return new PropertySet<>(mapKeys, where.and(andWhere));
    }

    public boolean isEmpty() {
        return where.isFalse();
    }

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return where.hashOuter(hashContext)*31 + AbstractOuterContext.hashOuter(mapKeys, hashContext);
    }

    public boolean equalsInner(PropertySet<T> object) {
        return hashEquals(where, object.where) && hashEquals(mapKeys, object.mapKeys);
    }

    protected PropertySet<T> translate(MapTranslate translator) {
        return new PropertySet<T>(translator.translateRevValues(mapKeys), where.translateOuter(translator));
    }

    public Where getWhere(ImMap<T, ? extends Expr> joinImplement) {
        return join(joinImplement).getWhere();
    }

    public Join<String> join(ImMap<T, ? extends Expr> joinImplement) {
        return getQuery().join(joinImplement);
    }

    public ImSet<ParamExpr> getKeys() {
        return BaseUtils.immutableCast(mapKeys.valuesSet());
    }

    public ImSet<Value> getValues() {
        return where.getOuterValues();
    }

}
