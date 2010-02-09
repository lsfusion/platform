package platform.server.data.expr.cases;

import platform.server.data.expr.BaseExpr;
import platform.server.data.where.Where;

import java.util.HashMap;
import java.util.Map;

public class MapCase<K> extends Case<Map<K, BaseExpr>> {

    MapCase() {
        super(Where.TRUE,new HashMap<K, BaseExpr>());
    }

    MapCase(Where iWhere, Map<K, BaseExpr> iData) {
        super(iWhere, iData);
    }
}
