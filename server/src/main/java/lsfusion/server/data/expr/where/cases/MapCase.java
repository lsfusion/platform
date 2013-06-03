package lsfusion.server.data.expr.where.cases;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.Case;
import lsfusion.server.data.where.Where;

public class MapCase<K> extends Case<ImMap<K, Expr>> {

    public MapCase(Where where, ImMap<K, Expr> data) {
        super(where, data);
    }
}
