package platform.server.session;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.caches.*;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.Query;
import platform.server.data.query.Join;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.util.HashSet;
import java.util.Map;

public class PropertyChange<T extends PropertyInterface> extends AbstractInnerContext<PropertyChange<T>> {
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

    public PropertyChange(Map<T, KeyExpr> mapKeys, Where where) {
        this(mapKeys, Expr.NULL, where);
    }

    public QuickSet<KeyExpr> getKeys() {
        return new QuickSet<KeyExpr>(mapKeys.values());
    }

    public QuickSet<Value> getValues() {
        return expr.getOuterValues().merge(where.getOuterValues());
    }

    public PropertyChange<T> and(Where andWhere) {
        return new PropertyChange<T>(mapKeys, expr, where.and(andWhere));
    }

    public <P extends PropertyInterface> PropertyChange<P> map(Map<P,T> mapping) {
        return new PropertyChange<P>(BaseUtils.join(mapping,mapKeys),expr,where);
    }

    public boolean isEmpty() {
        return where.isFalse();
    }

    public PropertyChange<T> add(PropertyChange<T> change) {
        if(isEmpty())
            return change;
        if(change.isEmpty())
            return this;
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

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return where.hashOuter(hashContext)*31*31 + expr.hashOuter(hashContext)*31 + AbstractOuterContext.hashOuter(mapKeys, hashContext);
    }

    public boolean equalsInner(PropertyChange<T> object) {
        return BaseUtils.hashEquals(where,object.where) && BaseUtils.hashEquals(expr,object.expr);
    }

    protected PropertyChange<T> translate(MapTranslate translator) {
        return new PropertyChange<T>(translator.translateKey(mapKeys),expr.translateOuter(translator),where.translateOuter(translator));
    }

    public long getComplexity(boolean outer) {
        return where.getComplexity(outer) + expr.getComplexity(outer);
    }
    public PropertyChange<T> pack() {
        Where packWhere = where.pack();
        return new PropertyChange<T>(mapKeys, expr.followFalse(packWhere.not(), true), packWhere);
    }

    public Expr getExpr(Map<T, ? extends Expr> joinImplement, WhereBuilder where) {
        Join<String> join = join(joinImplement);
        if(where !=null) where.add(join.getWhere());
        return join.getExpr("value");
    }

    public static <P extends PropertyInterface> PropertyChange<P> addNull(PropertyChange<P> change1, PropertyChange<P> change2) {
        if(change1==null)
            return change2;
        if(change2==null)
            return change1;
        return change1.add(change2);
    }

    public PropertyChange<T> correctIncrement(Property<T> property) {
        return new PropertyChange<T>(mapKeys, expr, where.and(expr.getWhere().or(property.getExpr(mapKeys).getWhere())));
    }
    
    public StatKeys<T> getStatKeys() {
        return where.getStatKeys(getInnerKeys()).mapBack(mapKeys);
    }
}
