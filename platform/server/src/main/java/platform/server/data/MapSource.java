package platform.server.data;

import platform.server.data.query.exprs.ValueExpr;

import java.util.Map;

public class MapSource<K,V,MK,MV> {

    public final Map<K,MK> mapKeys;
    public final Map<V,MV> mapProps;
    public final Map<ValueExpr,ValueExpr> mapValues;

    public MapSource(Map<K, MK> iMapKeys, Map<V, MV> iMapProps, Map<ValueExpr,ValueExpr> iMapValues) {
        mapKeys = iMapKeys;
        mapProps = iMapProps;
        mapValues = iMapValues;
    }
}
