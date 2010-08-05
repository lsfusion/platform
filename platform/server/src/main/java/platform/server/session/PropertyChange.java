package platform.server.session;

import platform.base.BaseUtils;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValues;
import platform.server.caches.TwinsInnerContext;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashValues;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.Query;
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

    public Set<KeyExpr> getKeys() {
        return new HashSet<KeyExpr>(mapKeys.values());
    }

    @IdentityLazy
    public Set<ValueExpr> getValues() {
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
        throw new RuntimeException("not supported yet");
    }

    public <V> Query<T,V> getQuery(V value) {
        Query<T,V> query = new Query<T, V>(mapKeys); // через query для кэша
        query.and(where);
        query.properties.put(value,expr);
        return query;
   }

    @IdentityLazy
    public int hashInner(HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<T,KeyExpr> mapKey : mapKeys.entrySet())
            hash += mapKey.getKey().hashCode() ^ mapKey.getValue().hashOuter(hashContext);

        return where.hashOuter(hashContext)*31*31 + expr.hashOuter(hashContext)*31 + hash;
    }

    public boolean equalsInner(PropertyChange<T> object) {
        return where.equals(object.where) && expr.equals(object.expr);
    }

    @IdentityLazy
    public int hashValues(final HashValues hashValues) {
        return hashInner(hashValues);
    }

    public PropertyChange<T> translateInner(MapTranslate translator) {
        return new PropertyChange<T>(translator.translateKey(mapKeys),expr.translateOuter(translator),where.translateOuter(translator));
    }

    public PropertyChange<T> translate(MapValuesTranslate mapValues) {
        return translateInner(mapValues.mapKeys());
    }

}
