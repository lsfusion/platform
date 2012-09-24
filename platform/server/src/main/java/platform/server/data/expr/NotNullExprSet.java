package platform.server.data.expr;

import platform.base.BaseUtils;
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
    
    public QuickSet<InnerExpr> getInnerExprs() {
        boolean hasNotInner = false;
        for(int i=0;i<size;i++) // оптимизация
            if(!(get(i) instanceof InnerExpr)) {
                hasNotInner = true;
                break;
            }
        if(!hasNotInner)
            return BaseUtils.immutableCast(this);

        QuickSet<InnerExpr> result = new QuickSet<InnerExpr>();
        for(int i=0;i<size;i++) {
            NotNullExpr expr = get(i);
            if(expr instanceof InnerExpr)
                result.add((InnerExpr)expr);
            else
                result.addAll(expr.getExprFollows(false).getInnerExprs());
        }
        return result;
    }
}
