package platform.server.data.expr.cases;

import platform.server.data.expr.BaseExpr;
import platform.server.data.where.Where;

import java.util.HashMap;
import java.util.Map;

public class MapCase<K> extends Case<Map<K, BaseExpr>> {

    public MapCase() {
        super(Where.TRUE,new HashMap<K, BaseExpr>());
    }

    public MapCase(Where where, Map<K, BaseExpr> data) {
        super(where, data);
    }
}
