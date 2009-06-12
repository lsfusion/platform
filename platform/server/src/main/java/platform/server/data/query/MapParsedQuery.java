package platform.server.data.query;

import platform.server.data.MapSource;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.where.Where;
import platform.base.BaseUtils;

import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Map;

public class MapParsedQuery<K,V,MK,MV> extends MapJoin<V,MV> implements ParsedQuery<K,V> {

    final ParsedQuery<MK,MV> query;

    final Map<K,MK> mapKeys;
    // какой нужен\какой есть в query
    final Map<ValueExpr, ValueExpr> mapValues;

    MapParsedQuery(ParsedJoinQuery<MK,MV> iQuery, MapSource<K,V,MK,MV> mapSource) {
        this(iQuery,mapSource.mapProps,mapSource.mapKeys,mapSource.mapValues);
    }

    MapParsedQuery(ParsedQuery<MK,MV> iQuery,Map<V,MV> iMapProps,Map<K,MK> iMapKeys,Map<ValueExpr,ValueExpr> iMapValues) {
        super(iQuery,iMapProps);

        query = iQuery;
        mapKeys = iMapKeys;
        mapValues = iMapValues;
    }

    public CompiledQuery<K, V> compileSelect(SQLSyntax syntax, LinkedHashMap<V, Boolean> orders, int top) {
        return new CompiledQuery<K,V>(query.compileSelect(syntax,BaseUtils.linkedJoin(orders,mapProps),top),mapKeys,mapProps,mapValues);
    }

    public <B> ClassWhere<B> getClassWhere(Collection<? extends V> classProps) {
        // нужно перемаппить ClassWhere, здесь по большому счету не нужен mapValues потому как assert то классы совпадают
        return (ClassWhere<B>) query.getClassWhere(BaseUtils.filterKeys(mapProps,classProps).values()).mapKeys(BaseUtils.reverse(BaseUtils.merge(mapProps,mapKeys)));
    }

    public Collection<ValueExpr> getValues() {
        return mapValues.keySet();
    }

    public Join<V> join(Map<K, ? extends SourceExpr> joinImplement) {
        return join(joinImplement,BaseUtils.toMap(getValues()));
    }

    public Join<V> join(Map<K, ? extends SourceExpr> joinImplement, Map<ValueExpr, ValueExpr> joinValues) {
        return new MapJoin<V,MV>(query.join(BaseUtils.crossJoin(mapKeys,joinImplement),BaseUtils.crossJoin(mapValues,joinValues)),mapProps);
    }

    public <GK extends V, GV extends V> MapParsedQuery<GK, GV, MV, MV> groupBy(Collection<GK> keys, Collection<GV> max, Collection<GV> sum) {
        Map<GK, MV> mapPropKeys = BaseUtils.filterKeys(mapProps, keys);
        Map<GV, MV> mapPropsMax = BaseUtils.filterKeys(mapProps, max);
        Map<GV, MV> mapPropsSum = BaseUtils.filterKeys(mapProps, sum);
        ParsedQuery<MV, MV> groupQuery = query.groupBy(mapPropKeys.values(), mapPropsMax.values(), mapPropsSum.values());
        return new MapParsedQuery<GK, GV, MV, MV>(groupQuery,BaseUtils.merge(mapPropsMax,mapPropsSum),mapPropKeys,BaseUtils.toMap(groupQuery.getValues()));
    }

    public Map<K, KeyExpr> getMapKeys() {
        return BaseUtils.join(mapKeys,query.getMapKeys());
    }
}
