package platform.server.data.expr;

import platform.base.QuickSet;
import platform.server.data.query.InnerJoins;

import java.util.Collection;

public class InnerExprSet extends QuickSet<InnerExpr> {

    public InnerExprSet() {
    }

    public InnerExprSet(InnerExpr expr) {
        add(expr);
    }

    public InnerExprSet(QuickSet<InnerExpr> set) {
        super(set);
    }

    public InnerExprSet(InnerExprSet[] sets) {
        super(sets);
    }

    public InnerExprSet(Collection<BaseExpr> exprs, boolean recursive) {
        for(BaseExpr expr : exprs)
            addAll(expr.getExprFollows(true, recursive));
    }
}
