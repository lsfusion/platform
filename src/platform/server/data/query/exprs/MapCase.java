package platform.server.data.query.exprs;

import java.util.Map;
import java.util.HashMap;

import platform.server.where.Where;

public class MapCase<K> extends Case<Map<K,AndExpr>> {

    MapCase() {
        super(Where.TRUE,new HashMap<K,AndExpr>());
    }

    MapCase(Where iWhere, Map<K, AndExpr> iData) {
        super(iWhere, iData);
    }
}
