package platform.server.session;

import platform.base.BaseUtils;
import platform.server.caches.*;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.Query;
import platform.server.data.query.Join;
import platform.server.data.translator.HashLazy;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;
import platform.server.logics.property.PropertyInterface;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PropertyChange<T extends PropertyInterface> extends TwinsInnerContext<PropertyChange<T>> implements MapValues<PropertyChange<T>> {
    public final Map<T,KeyExpr> mapKeys;
    public final Expr expr;
    public final Where where;

    public PropertyChange(Map<T, KeyExpr> mapKeys, Expr expr, Where where) {
        assert !mapKeys.containsValue(null);
        this.mapKeys = mapKeys;
        this.expr = expr;
        this.where = where;
    }

    public PropertyChange(Map<T, KeyExpr> mapKeys, Expr expr) {
        this(mapKeys, expr, expr.getWhere());
    }

    public Set<KeyExpr> getKeys() {
        return new HashSet<KeyExpr>(mapKeys.values());
    }

    @IdentityLazy
    public Set<Value> getValues() {
        return AbstractSourceJoin.enumValues(expr,where);
    }

    public PropertyChange<T> and(Where andWhere) {
        return new PropertyChange<T>(mapKeys, expr, where.and(andWhere));
    }

    public <P extends PropertyInterface> PropertyChange<P> map(Map<P,T> mapping) {
        return new PropertyChange<P>(BaseUtils.join(mapping,mapKeys),expr,where);
    }

    public PropertyChange<T> add(PropertyChange<T> change) {
        if(equals(change))
            return this;

        // assert что addJoin.getWhere() не пересекается с where, в общем случае что по пересекаемым они совпадают
        Join<String> addJoin = change.join(mapKeys);
        return new PropertyChange<T>(mapKeys, expr.ifElse(where, addJoin.getExpr("value")), where.or(addJoin.getWhere()));
    }

    public Where getWhere(Map<T, ? extends Expr> joinImplement) {
        return join(joinImplement).getWhere();
    }

    public Join<String> join(Map<T, ? extends Expr> joinImplement) {
        return getQuery().join(joinImplement);
    }

    @IdentityLazy
    public Query<T,String> getQuery() {
        Query<T,String> query = new Query<T, String>(mapKeys); // через query для кэша
        query.and(where);
        query.properties.put("value",expr);
        return query;
   }

    @HashLazy
    public int hashInner(HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<T,KeyExpr> mapKey : mapKeys.entrySet())
            hash += mapKey.getKey().hashCode() ^ mapKey.getValue().hashOuter(hashContext);

        return where.hashOuter(hashContext)*31*31 + expr.hashOuter(hashContext)*31 + hash;
    }

    public boolean equalsInner(PropertyChange<T> object) {
        return BaseUtils.hashEquals(where,object.where) && BaseUtils.hashEquals(expr,object.expr);
    }

    @HashLazy
    public int hashValues(final HashValues hashValues) {
        return hashInner(hashValues);
    }

    public PropertyChange<T> translateInner(MapTranslate translator) {
        return new PropertyChange<T>(translator.translateKey(mapKeys),expr.translateOuter(translator),where.translateOuter(translator));
    }

    public PropertyChange<T> translate(MapValuesTranslate mapValues) {
        return translateInner(mapValues.mapKeys());
    }

    private BaseUtils.HashComponents<Value> components = null;
    @ManualLazy
    public BaseUtils.HashComponents<Value> getComponents() {
        if(components==null)
            components = AbstractMapValues.getComponents(this);
        return components;
    }

    public long getComplexity() {
        return where.getComplexity() + expr.getComplexity();
    }
    public PropertyChange<T> pack() {
        Where packWhere = where.pack();
        return new PropertyChange<T>(mapKeys, expr.followFalse(packWhere.not(), true), packWhere);
    }
}
