package platform.server.data.query;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapQuery<K,V,MK,MV> extends IQuery<K,V> {

    final Query<MK,MV> query;

    final Map<V,MV> mapProps;
    final Map<K,MK> mapKeys;
    // какой есть в query -> какой нужен
    final MapValuesTranslate mapValues;

    public Expr getExpr(V property) {
        return query.getExpr(mapProps.get(property)).translateOuter(mapValues.mapKeys());
    }

    public Set<V> getProperties() {
        return mapProps.keySet();
    }

    public Map<K, KeyExpr> getMapKeys() {
        return BaseUtils.join(mapKeys, query.getMapKeys());
    }

    public MapQuery(Query<MK, MV> query, Map<V, MV> mapProps, Map<K, MK> mapKeys, MapValuesTranslate mapValues) {
        this.query = query;
        this.mapProps = mapProps;
        this.mapKeys = mapKeys;
        this.mapValues = mapValues;
    }

    public CompiledQuery<K, V> compile(SQLSyntax syntax, OrderedMap<V, Boolean> orders, Integer top, SubQueryContext subcontext, boolean recursive) {
        return new CompiledQuery<K,V>(query.compile(syntax, orders.map(mapProps), top, subcontext, recursive),mapKeys,mapProps,mapValues);
    }

    public <B> ClassWhere<B> getClassWhere(Set<? extends V> classProps) {
        // нужно перемаппить ClassWhere, здесь по большому счету не нужен mapValues потому как assert то классы совпадают
        return (ClassWhere<B>) new ClassWhere<Object>(query.getClassWhere(new HashSet<MV>(BaseUtils.filterKeys(mapProps, classProps).values())), BaseUtils.reverse(BaseUtils.merge(mapProps, mapKeys)));
    }

    public Join<V> join(Map<K, ? extends Expr> joinImplement, MapValuesTranslate joinValues) {
        assert joinValues.assertValuesEquals(getInnerValues().getSet());
        return new RemapJoin<V,MV>(query.join(BaseUtils.crossJoin(mapKeys,joinImplement),mapValues.map(joinValues)),mapProps);
    }
    public Join<V> joinExprs(Map<K, ? extends Expr> joinImplement, MapValuesTranslate joinValues) {
        assert joinValues.assertValuesEquals(getInnerValues().getSet());
        return new RemapJoin<V,MV>(query.joinExprs(BaseUtils.crossJoin(mapKeys,joinImplement),mapValues.map(joinValues)),mapProps);
    }

    public PullValues<K, V> pullValues() {
        PullValues<MK, MV> pullValues = query.pullValues();
        if(pullValues.isEmpty())
            return new PullValues<K, V>(this);

        return pullValues.map(mapKeys, mapProps, mapValues);
    }

    public long getComplexity(boolean outer) {
        return query.getComplexity(outer);
    }

    public IQuery<K, V> calculatePack() {
        IQuery<MK, MV> packedQuery = query.pack();
        if(packedQuery!=query)
            return packedQuery.map(mapKeys, mapProps, mapValues);
        else
            return this;
    }

    @IdentityLazy
    public Query<K, V> getQuery() {
        Query<MK, MV> transQuery = query.translateQuery(mapValues.mapKeys());
        return new Query<K,V>(BaseUtils.join(mapKeys, query.mapKeys), BaseUtils.join(mapProps, transQuery.properties), transQuery.where);
    }

    public <RMK, RMV> IQuery<RMK, RMV> map(Map<RMK, K> remapKeys, Map<RMV, V> remapProps, MapValuesTranslate translate) {
        return new MapQuery<RMK, RMV, MK, MV>(query, BaseUtils.join(remapProps, mapProps), BaseUtils.join(remapKeys, mapKeys), translate.map(mapValues));
    }

    public MapQuery<K, V, ?, ?> translateMap(MapValuesTranslate translate) {
        return new MapQuery<K,V,MK,MV>(query, mapProps, mapKeys, mapValues.map(translate));
    }
    public IQuery<K, V> translateQuery(MapTranslate translate) {
        return new MapQuery<K,V,MK,MV>(query.translateQuery(translate.onlyKeys()), mapProps, mapKeys, mapValues.map(translate.mapValues()));
    }

    protected QuickSet<KeyExpr> getKeys() {
        return query.getInnerKeys();
    }

    public Where getWhere() {
        return query.getWhere().translateOuter(mapValues.mapKeys());
    }

    public QuickSet<Value> getValues() {
        return mapValues.translateValues(query.getInnerValues());
    }

    protected int hash(HashContext hash) {
        return getQuery().hash(hash);
    }

    public boolean equalsInner(IQuery<K, V> object) {
        return getQuery().equalsInner(object);
    }
}
