package platform.server.data.query;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.MapValuesTranslator;
import platform.server.data.where.classes.ClassWhere;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MapParsedQuery<K,V,MK,MV> implements ParsedQuery<K,V> {

    final ParsedQuery<MK,MV> query;

    final Map<V,MV> mapProps;
    final Map<K,MK> mapKeys;
    // какой есть в query -> какой нужен
    final MapValuesTranslate mapValues;

    public MapParsedQuery(ParsedQuery<MK,MV> query,Map<V,MV> mapProps,Map<K,MK> mapKeys,MapValuesTranslate mapValues) {
        this.query = query;
        this.mapProps = mapProps;
        this.mapKeys = mapKeys;
        this.mapValues = mapValues;
    }

    public CompiledQuery<K, V> compileSelect(SQLSyntax syntax, OrderedMap<V,Boolean> orders, int top) {
        return new CompiledQuery<K,V>(query.compileSelect(syntax,orders.map(mapProps),top),mapKeys,mapProps,mapValues);
    }

    public <B> ClassWhere<B> getClassWhere(Collection<? extends V> classProps) {
        // нужно перемаппить ClassWhere, здесь по большому счету не нужен mapValues потому как assert то классы совпадают
        return (ClassWhere<B>) new ClassWhere<Object>(query.getClassWhere(BaseUtils.filterKeys(mapProps, classProps).values()), BaseUtils.reverse(BaseUtils.merge(mapProps, mapKeys)));
    }
    
    public Set<ValueExpr> getValues() {
        return mapValues.translateValues(query.getValues());
    }

    public Join<V> join(Map<K, ? extends Expr> joinImplement) {
        return join(joinImplement, MapValuesTranslator.noTranslate);
    }

    public Join<V> join(Map<K, ? extends Expr> joinImplement, MapValuesTranslate joinValues) {
        assert joinValues.assertValuesEquals(getValues());
        return new MapJoin<V,MV>(query.join(BaseUtils.crossJoin(mapKeys,joinImplement),mapValues.map(joinValues)),mapProps);
    }
}
