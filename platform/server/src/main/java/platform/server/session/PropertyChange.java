package platform.server.session;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.where.Where;
import platform.server.data.query.Query;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashValues;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.translator.DirectTranslator;
import platform.server.logics.property.PropertyInterface;
import platform.server.caches.*;
import platform.base.BaseUtils;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@GenericImmutable
public class PropertyChange<T extends PropertyInterface> extends AbstractMapValues<PropertyChange<T>> implements MapContext {
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

    @GenericLazy
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

    @GenericLazy
    public int hash(HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<T,KeyExpr> mapKey : mapKeys.entrySet())
            hash += mapKey.getKey().hashCode() ^ mapKey.getValue().hashContext(hashContext);

        return where.hashContext(hashContext)*31*31 + expr.hashContext(hashContext)*31 + hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyChange)) return false;

        PropertyChange<?> change = (PropertyChange)o;
        for(DirectTranslator translator : new MapHashIterable(this, change, false))
            if(where.translateDirect(translator).equals(change.where) && expr.translateDirect(translator).equals(change.expr))
                return true;
        return false;
    }

    @GenericLazy
    public int hashValues(final HashValues hashValues) {
        return hash(hashValues.mapKeys());
    }

    public PropertyChange<T> translate(DirectTranslator translator) {
        return new PropertyChange<T>(BaseUtils.join(mapKeys,translator.keys),expr.translateDirect(translator),where.translateDirect(translator));
    }

    public PropertyChange<T> translate(Map<ValueExpr,ValueExpr> mapValues) {
        return translate(new DirectTranslator(BaseUtils.toMap(BaseUtils.reverse(mapKeys).keySet()), mapValues));
    }
}
