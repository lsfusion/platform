package platform.server.data.query;

import platform.server.data.query.exprs.ValueExpr;

import java.util.HashMap;
import java.util.Map;

// поиск в кэше
class JoinCache<K,V> {
    JoinQuery<K,V> in;

    ParsedQuery<K,V> out;

    JoinCache(JoinQuery<K, V> iIn, ParsedQuery<K, V> iOut) {
        in = iIn;
        out = iOut;
    }

    <CK,CV> ParsedQuery<CK,CV> cache(JoinQuery<CK,CV> query) {
        Map<CK,K> mapKeys = new HashMap<CK,K>();
        Map<CV,V> mapProps = new HashMap<CV,V>();
        Map<ValueExpr,ValueExpr> mapValues = new HashMap<ValueExpr, ValueExpr>();
        if(query.equalsMap(in,mapKeys,mapProps,mapValues)) // нашли нужный кэш
            return new ParsedQuery<CK,CV>(out,mapKeys,mapProps,mapValues);

        return null;
    }
}
