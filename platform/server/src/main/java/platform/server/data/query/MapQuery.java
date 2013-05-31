package platform.server.data.query;

import platform.base.Pair;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.IdentityInstanceLazy;
import platform.server.caches.ParamExpr;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;

public class MapQuery<K,V,MK,MV> extends IQuery<K,V> {

    final Query<MK,MV> query;

    final ImRevMap<V,MV> mapProps;
    final ImRevMap<K,MK> mapKeys;
    // какой есть в query -> какой нужен
    final MapValuesTranslate mapValues;

    public Expr getExpr(V property) {
        return query.getExpr(mapProps.get(property)).translateOuter(mapValues.mapKeys());
    }

    public ImSet<V> getProperties() {
        return mapProps.keys();
    }

    public ImRevMap<K, KeyExpr> getMapKeys() {
        return mapKeys.join(query.getMapKeys());
    }

    public MapQuery(Query<MK, MV> query, ImRevMap<V, MV> mapProps, ImRevMap<K, MK> mapKeys, MapValuesTranslate mapValues) {
        this.query = query;
        this.mapProps = mapProps;
        this.mapKeys = mapKeys;
        this.mapValues = mapValues;

//        assert mapValues.assertValuesEquals(query.getInnerValues()); // все должны быть параметры
    }

    public CompiledQuery<K, V> compile(SQLSyntax syntax, ImOrderMap<V, Boolean> orders, Integer top, SubQueryContext subcontext, boolean recursive) {
        return new CompiledQuery<K,V>(query.compile(syntax, orders.map(mapProps), top, subcontext, recursive),mapKeys,mapProps,mapValues);
    }

    public ImOrderMap<V, CompileOrder> getCompileOrders(ImOrderMap<V, Boolean> orders) {
        return query.getCompileOrders(orders.map(mapProps)).map(mapProps.reverse());
    }

    public <B> ClassWhere<B> getClassWhere(ImSet<? extends V> classProps) {
        // нужно перемаппить ClassWhere, здесь по большому счету не нужен mapColValues потому как assert то классы совпадают
        return (ClassWhere<B>) new ClassWhere<Object>(query.getClassWhere(((ImSet<V>)classProps).mapRev(mapProps)), MapFact.addRevExcl(mapProps, mapKeys).reverse());
    }
    public Pair<IQuery<K, Object>, ImRevMap<Expr, Object>> getClassQuery(BaseClass baseClass) {
        Pair<IQuery<MK, Object>, ImRevMap<Expr, Object>> classQuery = query.getClassQuery(baseClass);

        return new Pair<IQuery<K, Object>, ImRevMap<Expr, Object>>(
                new MapQuery<K, Object, MK, Object>((Query<MK, Object>)classQuery.first, classQuery.second.valuesSet().toRevMap().addRevExcl(mapProps), mapKeys, mapValues),
                mapValues.mapKeys().translateExprRevKeys(classQuery.second));
    }

    public Join<V> join(ImMap<K, ? extends Expr> joinImplement, MapValuesTranslate joinValues) {
        assert joinValues.assertValuesEquals(getInnerValues());
        return new RemapJoin<V,MV>(query.join(mapKeys.crossJoin(joinImplement),mapValues.map(joinValues)),mapProps);
    }
    public Join<V> joinExprs(ImMap<K, ? extends Expr> joinImplement, MapValuesTranslate joinValues) {
        assert joinValues.assertValuesEquals(getInnerValues());
        return new RemapJoin<V,MV>(query.joinExprs(mapKeys.crossJoin(joinImplement),mapValues.map(joinValues)),mapProps);
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

    @IdentityInstanceLazy
    public Query<K, V> getQuery() {
        Query<MK, MV> transQuery = query.translateQuery(mapValues.mapKeys());
        return new Query<K,V>(mapKeys.join(query.mapKeys), mapProps.join(transQuery.properties), transQuery.where);
    }

    public <RMK, RMV> IQuery<RMK, RMV> map(ImRevMap<RMK, K> remapKeys, ImRevMap<RMV, V> remapProps, MapValuesTranslate translate) {
        return new MapQuery<RMK, RMV, MK, MV>(query, remapProps.join(mapProps), remapKeys.join(mapKeys), mapValues.map(translate));
    }

    public MapQuery<K, V, ?, ?> translateMap(MapValuesTranslate translate) {
        return new MapQuery<K,V,MK,MV>(query, mapProps, mapKeys, mapValues.map(translate));
    }
    public IQuery<K, V> translateQuery(MapTranslate translate) {
        return new MapQuery<K,V,MK,MV>(query.translateQuery(translate.onlyKeys()), mapProps, mapKeys, mapValues.map(translate.mapValues()));
    }

    protected ImSet<ParamExpr> getKeys() {
        return query.getInnerKeys();
    }

    public Where getWhere() {
        return query.getWhere().translateOuter(mapValues.mapKeys());
    }

    public ImSet<Value> getValues() {
        return mapValues.translateValues(query.getInnerValues());
    }

    protected int hash(HashContext hash) {
        return getQuery().hash(hash);
    }

    public boolean equalsInner(IQuery<K, V> object) {
        return getQuery().equalsInner(object);
    }
}
