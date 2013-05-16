package platform.server.data.expr.where.cases;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.Case;
import platform.server.data.where.Where;

public class MapCase<K> extends Case<ImMap<K, Expr>> {

    public MapCase(Where where, ImMap<K, Expr> data) {
        super(where, data);
    }
}
