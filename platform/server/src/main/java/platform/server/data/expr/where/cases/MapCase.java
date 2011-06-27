package platform.server.data.expr.where.cases;

import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.where.Case;
import platform.server.data.where.Where;

import java.util.HashMap;
import java.util.Map;

public class MapCase<K> extends Case<Map<K, BaseExpr>> {

    public MapCase() {
        super(Where.TRUE,new HashMap<K, BaseExpr>());
    }

    private Where upWhere = null;
    public MapCase(Where where, Map<K, BaseExpr> data, Where upWhere) {
        super(where, data);
        this.upWhere = upWhere;
    }

    public Where getExclWhere() {
        return where.and(upWhere.not());
    }
}
