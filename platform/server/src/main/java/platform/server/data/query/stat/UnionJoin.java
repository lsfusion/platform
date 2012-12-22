package platform.server.data.query.stat;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.base.TwinImmutableObject;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MOrderExclMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.add.MAddMap;
import platform.server.caches.IdentityLazy;
import platform.server.caches.OuterContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.QueryExpr;
import platform.server.data.expr.query.QueryJoin;
import platform.server.data.query.ExprEnumerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UnionJoin extends CalculateJoin<Integer> {

    private final ImSet<Expr> exprs;

    public UnionJoin(ImSet<Expr> exprs) {
        this.exprs = exprs;
    }

    @IdentityLazy
    public ImMap<Integer, BaseExpr> getJoins() {
        return ListFact.fromJavaCol(getCommonExprs()).toSet().toOrderSet().toIndexedMap();
    }

    private static void fillOrderedExprs(BaseExpr baseExpr, BaseExpr fromExpr, MOrderExclMap<BaseExpr, MSet<BaseExpr>> orderedExprs) {
        MSet<BaseExpr> fromExprs = orderedExprs.get(baseExpr);
        if(fromExprs == null) {
            for(BaseExpr joinExpr : baseExpr.getUsed())
                fillOrderedExprs(joinExpr, baseExpr, orderedExprs);
            fromExprs = SetFact.mSet();
            orderedExprs.exclAdd(baseExpr, fromExprs);
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

        MOrderExclMap<BaseExpr, MSet<BaseExpr>> mOrderedExprs = MapFact.mOrderExclMap();
        for(BaseExpr baseExpr : baseExprs)
            fillOrderedExprs(baseExpr, null, mOrderedExprs);
        ImOrderMap<BaseExpr,ImSet<BaseExpr>> orderedExprs = MapFact.immutable(mOrderedExprs);

        MAddMap<BaseExpr, ImSet<BaseExpr>> found = MapFact.mAddExclMapMax(orderedExprs.size());
        List<BaseExpr> result = new ArrayList<BaseExpr>();
        for(int i=orderedExprs.size()-1;i>=0;i--) { // бежим с конца
            BaseExpr baseExpr = orderedExprs.getKey(i);
            ImSet<BaseExpr> orderBaseExprs = orderedExprs.getValue(i);
            MSet<BaseExpr> mExprFound = SetFact.mSet();
            for(BaseExpr depExpr : orderBaseExprs) {
                ImSet<BaseExpr> prevSet = found.get(depExpr);
                if(prevSet==null) { // значит уже в result'е
                    mExprFound = null;
                    break;
                }
                mExprFound.addAll(prevSet);
            }
            if(baseExprs.contains(baseExpr))
                mExprFound.add(baseExpr); // assert'ся что не может быть exprFound
            ImSet<BaseExpr> exprFound = null;
            if(mExprFound!=null)
                exprFound = mExprFound.immutable();
            
            if(exprFound ==null || exprFound.size() == baseExprs.size()) { // все есть
                if(exprFound != null) // только что нашли
                    result.add(baseExpr);
            } else
                found.add(baseExpr, exprFound);
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

    public boolean twins(TwinImmutableObject o) {
        return exprs.equals(((UnionJoin)o).exprs);
    }

    public int immutableHashCode() {
        return exprs.hashCode();
    }
}
