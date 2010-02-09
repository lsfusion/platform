package platform.server.data.query;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.where.classes.ClassWhere;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.sql.SQLSyntax;

import java.util.Collection;
import java.util.Map;

public class MapParsedQuery<K,V,MK,MV> implements ParsedQuery<K,V> {

    final ParsedQuery<MK,MV> query;

    final Map<V,MV> mapProps;
    final Map<K,MK> mapKeys;
    // какой есть в query -> какой нужен
    final Map<ValueExpr, ValueExpr> mapValues;

    MapParsedQuery(ParsedQuery<MK,MV> iQuery,Map<V,MV> iMapProps,Map<K,MK> iMapKeys,Map<ValueExpr,ValueExpr> iMapValues) {
        query = iQuery;
        mapProps = iMapProps;
        mapKeys = iMapKeys;
        mapValues = iMapValues;
    }

    public CompiledQuery<K, V> compileSelect(SQLSyntax syntax, OrderedMap<V,Boolean> orders, int top) {
        return new CompiledQuery<K,V>(query.compileSelect(syntax,orders.map(mapProps),top),mapKeys,mapProps,mapValues);
    }

    public <B> ClassWhere<B> getClassWhere(Collection<? extends V> classProps) {
        // нужно перемаппить ClassWhere, здесь по большому счету не нужен mapValues потому как assert то классы совпадают
        return (ClassWhere<B>) new ClassWhere<Object>(query.getClassWhere(BaseUtils.filterKeys(mapProps, classProps).values()), BaseUtils.reverse(BaseUtils.merge(mapProps, mapKeys)));
    }

    private Collection<ValueExpr> getValues() {
        return mapValues.values();
    }

    public Join<V> join(Map<K, ? extends Expr> joinImplement) {
        return join(joinImplement,BaseUtils.toMap(getValues()));
    }

    public Join<V> join(Map<K, ? extends Expr> joinImplement, Map<ValueExpr, ValueExpr> joinValues) {
        return new MapJoin<V,MV>(query.join(BaseUtils.crossJoin(mapKeys,joinImplement),BaseUtils.join(mapValues,joinValues)),mapProps);
    }

}
