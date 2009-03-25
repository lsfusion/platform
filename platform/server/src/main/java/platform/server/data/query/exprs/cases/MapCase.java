package platform.server.data.query.exprs.cases;

import platform.server.where.Where;
import platform.server.data.query.exprs.AndExpr;

import java.util.HashMap;
import java.util.Map;

public class MapCase<K> extends Case<Map<K, AndExpr>> {

    MapCase() {
        super(Where.TRUE,new HashMap<K,AndExpr>());
    }

    MapCase(Where iWhere, Map<K, AndExpr> iData) {
        super(iWhere, iData);
    }
}
