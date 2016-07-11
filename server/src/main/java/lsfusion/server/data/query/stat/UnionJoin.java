package lsfusion.server.data.query.stat;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.QueryExpr;
import lsfusion.server.data.expr.query.QueryJoin;
import lsfusion.server.data.query.ExprEnumerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UnionJoin extends CalculateJoin<Integer> {

    private final ImSet<Expr> exprs;

    public UnionJoin(ImSet<Expr> exprs) {
        this.exprs = exprs;
    }

    public ImMap<Integer, BaseExpr> getJoins() {
        return getJoins(false);
    }

    @IdentityLazy
    public ImMap<Integer, BaseExpr> getJoins(boolean forStat) {
        ImSet<BaseExpr> commonExprs = ListFact.fromJavaCol(getCommonExprs()).toSet();
        if(forStat) {
            ImSet<ParamExpr> lostKeys = AbstractOuterContext.getOuterSetKeys(exprs).removeIncl(AbstractOuterContext.getOuterColKeys(commonExprs));
            commonExprs = commonExprs.addExcl(lostKeys);
        }
        return commonExprs.toOrderSet().toIndexedMap();
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

        Set<BaseExpr> baseExprs = new HashSet<>();
        for(Expr expr : exprs)
            baseExprs.addAll(expr.getBaseExprs());

        if(baseExprs.size()==1)
            return new ArrayList<>(baseExprs);

        MOrderExclMap<BaseExpr, MSet<BaseExpr>> mOrderedExprs = MapFact.mOrderExclMap();
        for(BaseExpr baseExpr : baseExprs)
            fillOrderedExprs(baseExpr, null, mOrderedExprs);
        ImOrderMap<BaseExpr,ImSet<BaseExpr>> orderedExprs = MapFact.immutable(mOrderedExprs);

        MAddMap<BaseExpr, ImSet<BaseExpr>> found = MapFact.mAddExclMapMax(orderedExprs.size());
        List<BaseExpr> result = new ArrayList<>();
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
    
    public static boolean depends(OuterContext context, final QueryJoin dependJoin) {
        final Result<Boolean> depends = new Result<>(false);
        context.enumerate(new ExprEnumerator() {
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
    
    public boolean depends(final QueryJoin dependJoin) {
        for(Expr expr : exprs)
            if(depends(expr, dependJoin))
                return true;
        return false;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return exprs.equals(((UnionJoin)o).exprs);
    }

    public int immutableHashCode() {
        return exprs.hashCode();
    }
}
