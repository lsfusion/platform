package platform.server.data.query.exprs;

import platform.server.where.Where;

import java.util.HashMap;
import java.util.Map;

public class MapCase<K> extends Case<Map<K,AndExpr>> {

    MapCase() {
        super(Where.TRUE,new HashMap<K,AndExpr>());
    }

    MapCase(Where iWhere, Map<K, AndExpr> iData) {
        super(iWhere, iData);
    }
}
