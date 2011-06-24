package platform.server.data.expr.cases;

import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;

import java.util.HashMap;
import java.util.Map;

public class MapCase<K> extends Case<Map<K, Expr>> {

    public MapCase() {
        super(Where.TRUE,new HashMap<K, Expr>());
    }

    private Where upWhere = null;
    public MapCase(Where where, Map<K, Expr> data, Where upWhere) {
        super(where, data);
        this.upWhere = upWhere;
    }

    public Where getExclWhere() {
        return where.and(upWhere.not());
    }
}
