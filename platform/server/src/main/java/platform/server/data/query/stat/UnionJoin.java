package platform.server.data.query.stat;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Result;
import platform.base.TwinImmutableInterface;
import platform.server.caches.IdentityLazy;
import platform.server.caches.OuterContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.QueryExpr;
import platform.server.data.expr.query.QueryJoin;
import platform.server.data.query.ExprEnumerator;

import java.util.*;

public class UnionJoin extends CalculateJoin<Integer> {

    private final Set<Expr> exprs;

    public UnionJoin(Set<Expr> exprs) {
        this.exprs = exprs;
    }

    @IdentityLazy
    public Map<Integer, BaseExpr> getJoins() {
        return BaseUtils.toMap(getCommonExprs());
    }

    private static void fillOrderedExprs(BaseExpr baseExpr, BaseExpr fromExpr, OrderedMap<BaseExpr, Collection<BaseExpr>> orderedExprs) {
        Collection<BaseExpr> fromExprs = orderedExprs.get(baseExpr);
        if(fromExprs == null) {
            for(BaseExpr joinExpr : baseExpr.getUsed())
                fillOrderedExprs(joinExpr, baseExpr, orderedExprs);
            fromExprs = new ArrayList<BaseExpr>();
            orderedExprs.put(baseExpr, fromExprs);
        }
        if(fromExpr!=null)
            fromExprs.add(fromExpr);
    }

    private List<BaseExpr> getCommonExprs() {

        Set<BaseExpr> baseExprs = new HashSet<BaseExpr>();
        for(Expr expr : exprs)
            baseExprs.addAll(expr.getBaseExprs());

        if(baseExprs.size()==1)
            return new ArrayList<BaseExpr>(baseExprs);

        Map<BaseExpr, Set<BaseExpr>> found = new HashMap<BaseExpr, Set<BaseExpr>>();
        OrderedMap<BaseExpr, Collection<BaseExpr>> orderedExprs = new OrderedMap<BaseExpr, Collection<BaseExpr>>();
        for(BaseExpr baseExpr : baseExprs)
            fillOrderedExprs(baseExpr, null, orderedExprs);

        List<BaseExpr> result = new ArrayList<BaseExpr>();
        for(BaseExpr baseExpr : BaseUtils.reverse(orderedExprs.keyList())) { // бежим с конца
            Set<BaseExpr> exprFound = new HashSet<BaseExpr>();
            for(BaseExpr depExpr : orderedExprs.get(baseExpr)) {
                Set<BaseExpr> prevSet = found.get(depExpr);
                if(prevSet==null) { // значит уже в result'е
                    exprFound = null;
                    break;
                }
                exprFound.addAll(prevSet);
            }
            if(baseExprs.contains(baseExpr))
                exprFound.add(baseExpr); // assert'ся что не может быть exprFound

            if(exprFound ==null || exprFound.size() == baseExprs.size()) { // все есть
                if(exprFound != null) // только что нашли
                    result.add(baseExpr);
            } else
                found.put(baseExpr, exprFound);
        }
        return result;
    }
    
    public boolean depends(final QueryJoin dependJoin) {
        final Result<Boolean> depends = new Result<Boolean>(false);
        for(Expr expr : exprs)
            expr.enumerate(new ExprEnumerator() {
                public Boolean enumerate(OuterContext join) {
                    if(join instanceof QueryExpr && BaseUtils.hashEquals(((QueryExpr) join).getInnerJoin(), dependJoin)) {
                        depends.set(true);
                        return null;
                    }
                    return true;
                }
            });
        return depends.result;
    }

    public boolean twins(TwinImmutableInterface o) {
        return exprs.equals(((UnionJoin)o).exprs);
    }

    public int immutableHashCode() {
        return exprs.hashCode();
    }
}
