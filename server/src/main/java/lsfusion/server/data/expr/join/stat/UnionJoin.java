package lsfusion.server.data.expr.join.stat;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.expr.ParamExpr;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.QueryExpr;
import lsfusion.server.data.expr.join.query.QueryJoin;
import lsfusion.server.data.expr.query.stat.Stat;
import lsfusion.server.data.expr.query.stat.StatType;
import lsfusion.server.data.query.ExprEnumerator;
import lsfusion.server.data.where.Where;

public class UnionJoin extends CalculateJoin<BaseExpr> {
    
    // тут возможно вообще надо было бы where, а не exprs хранить, но логика commonExprs все же требует логику Expr, а не Where (поэтому так, в частности, дублируется получение OrWhere, но там все редко используется и кэшируется)  
    private final ImSet<Expr> exprs;

    public UnionJoin(ImSet<Expr> exprs) {
        this.exprs = exprs;
    }

    public ImMap<BaseExpr, BaseExpr> getJoins() {
        return getJoins(false);
    }

    @IdentityLazy
    public ImMap<BaseExpr, BaseExpr> getJoins(boolean forStat) {
        ImSet<BaseExpr> commonExprs = getCommonExprs();
        if(forStat) {
            ImSet<ParamExpr> lostKeys = AbstractOuterContext.getOuterSetKeys(exprs).removeIncl(AbstractOuterContext.getOuterColKeys(commonExprs));
            commonExprs = commonExprs.addExcl(lostKeys);
//            joinExprs = getBaseExprs(); // нельзя, может докинуть новые join'ы (нарушит ряд инвариантов, начнет проталкивать эти докинутые join'ы и вообще будет фильтровать по сути по ним что неправильно)
        }
        return commonExprs.toRevMap();
    }
    
    @IdentityLazy
    private Where getOrWhere() {
        return Expr.getOrWhere(exprs);
    }

    @Override
    public Cost getPushedCost(KeyStat keyStat, StatType type, Cost pushCost, Stat pushStat, ImMap<BaseExpr, Stat> pushKeys, ImMap<BaseExpr, Stat> pushNotNullKeys, ImMap<BaseExpr, Stat> pushProps, Result<ImSet<BaseExpr>> rPushedKeys, Result<ImSet<BaseExpr>> rPushedProps) {
        if(pushKeys.size() < getJoins().size()) // не все ключи есть, запретим выбирать
            return Cost.ALOT;
        
        // можно было бы translate'ить commonExprs в ключи, но во-первых сама трансляция сложный механизм, во-вторых надо тогда докидывать еще и ключи не входящие в baseExpr, в общем схема будет по сложности равная проталкиванию, но учитывая что в PartitionJoin (наиболее похожем), ничего этого нет, то и здесь смысла нет

        StatKeys<BaseExpr> pushStatKeys = QueryJoin.adjustNotNullStats(pushCost, pushStat, pushKeys, pushNotNullKeys);
        return getPushedCost(type, pushStatKeys);
    }

    // важно делать IdentityLazy для мемоизации
    @IdentityLazy
    private Cost getPushedCost(StatType type, StatKeys<BaseExpr> pushStatKeys) {
        Where where = getOrWhere();
        return where.getPushedStatKeys(where.getOuterKeys(), type, pushStatKeys).getCost();
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

    private ImSet<BaseExpr> getCommonExprs() {
        return getCommonExprs(getBaseExprs());
    }

    private ImSet<BaseExpr> getBaseExprs() {
        MSet<BaseExpr> result = SetFact.mSet();
        for(Expr expr : exprs)
            result.addAll(expr.getBaseExprs());
        return result.immutable();
    }

    private static ImSet<BaseExpr> getCommonExprs(ImSet<BaseExpr> baseExprs) {
        if(baseExprs.size()==1)
            return baseExprs;

        MOrderExclMap<BaseExpr, MSet<BaseExpr>> mOrderedExprs = MapFact.mOrderExclMap();
        for(BaseExpr baseExpr : baseExprs)
            fillOrderedExprs(baseExpr, null, mOrderedExprs);
        ImOrderMap<BaseExpr,ImSet<BaseExpr>> orderedExprs = MapFact.immutable(mOrderedExprs);

        MAddExclMap<BaseExpr, ImSet<BaseExpr>> found = MapFact.mAddExclMapMax(orderedExprs.size());
        MSet<BaseExpr> mResult = SetFact.mSet();
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
                    mResult.add(baseExpr);
            } else
                found.exclAdd(baseExpr, exprFound);
        }
        return mResult.immutable();
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

    public static boolean depends(OuterContext context, final FunctionSet<BaseJoin> dependJoins) {
        final Result<Boolean> depends = new Result<>(false);
        context.enumerate(new ExprEnumerator() {
            public Boolean enumerate(OuterContext join) {
                if(join instanceof BaseExpr && dependJoins.contains(((BaseExpr) join).getBaseJoin())) {
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

    public boolean depends(final FunctionSet<BaseJoin> dependJoins) {
        for(Expr expr : exprs)
            if(depends(expr, dependJoins))
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
