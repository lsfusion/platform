package platform.server.data.expr;

import platform.base.QuickSet;
import platform.base.Result;
import platform.server.caches.ManualLazy;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.Map;

public class NotNullExprSet extends QuickSet<NotNullExpr> {

    public NotNullExprSet() {
    }

    public NotNullExprSet(NotNullExpr expr) {
        add(expr);
    }

    public NotNullExprSet(QuickSet<NotNullExpr> set) {
        super(set);
    }

    public NotNullExprSet(NotNullExprSet[] sets) {
        super(sets);
    }

    public NotNullExprSet(Collection<BaseExpr> exprs, boolean recursive) {
        for(BaseExpr expr : exprs)
            addAll(expr.getExprFollows(true, recursive));
    }
}
