package platform.server.data.query;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.QuickSet;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.MapValuesTranslator;
import platform.server.data.where.classes.ClassWhere;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapParsedQuery<K,V,MK,MV> implements ParsedQuery<K,V> {

    final ParsedQuery<MK,MV> query;

    final Map<V,MV> mapProps;
    final Map<K,MK> mapKeys;
    // какой есть в query -> какой нужен
    final MapValuesTranslate mapValues;

    public Expr getExpr(V property) {
        return query.getExpr(mapProps.get(property));
    }

    public MapParsedQuery(ParsedQuery<MK,MV> query,Map<V,MV> mapProps,Map<K,MK> mapKeys,MapValuesTranslate mapValues) {
        this.query = query;
        this.mapProps = mapProps;
        this.mapKeys = mapKeys;
        this.mapValues = mapValues;
    }

    public CompiledQuery<K, V> compileSelect(SQLSyntax syntax, OrderedMap<V, Boolean> orders, int top, String prefix) {
        return new CompiledQuery<K,V>(query.compileSelect(syntax,orders.map(mapProps),top,prefix),mapKeys,mapProps,mapValues);
    }

    public <B> ClassWhere<B> getClassWhere(Collection<? extends V> classProps) {
        // нужно перемаппить ClassWhere, здесь по большому счету не нужен mapValues потому как assert то классы совпадают
        return (ClassWhere<B>) new ClassWhere<Object>(query.getClassWhere(BaseUtils.filterKeys(mapProps, classProps).values()), BaseUtils.reverse(BaseUtils.merge(mapProps, mapKeys)));
    }
    
    public QuickSet<Value> getValues() {
        return mapValues.translateValues(query.getValues());
    }

    public Join<V> join(Map<K, ? extends Expr> joinImplement, MapValuesTranslate joinValues) {
        assert joinValues.assertValuesEquals(getValues().getSet());
        return new MapJoin<V,MV>(query.join(BaseUtils.crossJoin(mapKeys,joinImplement),mapValues.map(joinValues)),mapProps);
    }

    public Query<K, V> pullValues(Map<K, Expr> pullKeys, Map<V, Expr> pullProps) {
        Map<MK, Expr> mapPullKeys = new HashMap<MK, Expr>();
        Map<MV, Expr> mapPullProps = new HashMap<MV, Expr>();
        Query<MK, MV> mapQuery = query.pullValues(mapPullKeys, mapPullProps);
        pullKeys.putAll(BaseUtils.rightJoin(mapKeys, mapValues.mapKeys().translate(mapPullKeys)));
        pullProps.putAll(BaseUtils.rightJoin(mapProps, mapValues.mapKeys().translate(mapPullProps)));
        return new Query<K,V>(mapQuery.translateInner(mapValues.mapKeys()), BaseUtils.filterValues(mapKeys, mapQuery.mapKeys.keySet()), BaseUtils.filterValues(mapProps, mapQuery.properties.keySet()));
    }
}
