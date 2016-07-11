package lsfusion.server.data.query;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;

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

        assert mapValues.assertValuesContains(query.getInnerValues()); // например pack может убирать часть значений
    }

    public CompiledQuery<K, V> compile(ImOrderMap<V, Boolean> orders, CompileOptions<V> options) {
        return new CompiledQuery<>(query.compile(orders.map(mapProps), options.map(mapProps)), mapKeys, mapProps, mapValues);
    }

    public ImOrderMap<V, CompileOrder> getCompileOrders(ImOrderMap<V, Boolean> orders) {
        return query.getCompileOrders(orders.map(mapProps)).map(mapProps.reverse());
    }

    public <B> ClassWhere<B> getClassWhere(ImSet<? extends V> classProps) {
        // нужно перемаппить ClassWhere, здесь по большому счету не нужен mapColValues потому как assert то классы совпадают
        return (ClassWhere<B>) new ClassWhere<>(query.getClassWhere(((ImSet<V>) classProps).mapRev(mapProps)), MapFact.addRevExcl(mapProps, mapKeys).reverse());
    }
    public Pair<IQuery<K, Object>, ImRevMap<Expr, Object>> getClassQuery(BaseClass baseClass) {
        Pair<IQuery<MK, Object>, ImRevMap<Expr, Object>> classQuery = query.getClassQuery(baseClass);

        return new Pair<IQuery<K, Object>, ImRevMap<Expr, Object>>(
                new MapQuery<>((Query<MK, Object>) classQuery.first, classQuery.second.valuesSet().toRevMap().addRevExcl(mapProps), mapKeys, mapValues),
                mapValues.mapKeys().translateExprRevKeys(classQuery.second));
    }

    public Join<V> join(ImMap<K, ? extends Expr> joinImplement, MapValuesTranslate joinValues) {
        assert joinValues.assertValuesContains(getInnerValues());
        return new RemapJoin<>(query.join(mapKeys.crossJoin(joinImplement), mapValues.mapTrans(joinValues)), mapProps);
    }
    public Join<V> joinExprs(ImMap<K, ? extends Expr> joinImplement, MapValuesTranslate joinValues) {
        assert joinValues.assertValuesContains(getInnerValues());
        return new RemapJoin<>(query.joinExprs(mapKeys.crossJoin(joinImplement), mapValues.mapTrans(joinValues)), mapProps);
    }

    public PullValues<K, V> pullValues() {
        PullValues<MK, MV> pullValues = query.pullValues();
        if(pullValues.isEmpty())
            return new PullValues<>(this);

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
        return new Query<>(mapKeys.join(query.mapKeys), mapProps.join(transQuery.properties), transQuery.where);
    }

    public <RMK, RMV> IQuery<RMK, RMV> map(ImRevMap<RMK, K> remapKeys, ImRevMap<RMV, V> remapProps, MapValuesTranslate translate) {
        return new MapQuery<>(query, remapProps.join(mapProps), remapKeys.join(mapKeys), mapValues.mapTrans(translate));
    }

    public MapQuery<K, V, ?, ?> translateMap(MapValuesTranslate translate) {
        return new MapQuery<>(query, mapProps, mapKeys, mapValues.mapTrans(translate));
    }
    public IQuery<K, V> translateQuery(MapTranslate translate) {
        return new MapQuery<>(query.translateQuery(translate.onlyKeys()), mapProps, mapKeys, mapValues.mapTrans(translate.mapValues()));
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
