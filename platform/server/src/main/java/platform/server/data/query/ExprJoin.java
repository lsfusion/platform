package platform.server.data.query;

import platform.base.QuickSet;
import platform.base.Result;
import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.*;
import platform.server.data.query.stat.UnionJoin;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public abstract class ExprJoin<T extends ExprJoin<T>> extends AbstractOuterContext<T> implements WhereJoin<Integer, T> {

    protected final BaseExpr baseExpr;

    public ExprJoin(BaseExpr baseExpr) {
        this.baseExpr = baseExpr;
    }

    @Override
    public QuickSet<OuterContext> calculateOuterDepends() {
        return new QuickSet<OuterContext>(baseExpr);
    }

    protected int hash(HashContext hashContext) {
        return baseExpr.hashOuter(hashContext);
    }

    public boolean twins(TwinImmutableInterface o) {
        return baseExpr.equals(((ExprJoin) o).baseExpr);
    }

    public InnerJoins getJoinFollows(Result<Map<InnerJoin, Where>> upWheres, Collection<UnionJoin> unionJoins) { // все равно использует getExprFollows
        return InnerExpr.getFollowJoins(this, upWheres, unionJoins);
    }

    public NotNullExprSet getExprFollows(boolean recursive) {
        return InnerExpr.getExprFollows(this, recursive);
    }

    public Map<Integer, BaseExpr> getJoins() {
        return Collections.singletonMap(0, (BaseExpr) baseExpr);
    }

    public static InnerJoins getInnerJoins(BaseExpr baseExpr) {
        InnerJoins result = new InnerJoins();
        QuickSet<InnerExpr> innerExprs = baseExpr.getExprFollows(true, false).getInnerExprs(null);
        for(int i=0;i<innerExprs.size;i++)
            result = result.and(new InnerJoins(innerExprs.get(i).getInnerJoin()));
        return result;
    }

    public InnerJoins getInnerJoins() {
        return getInnerJoins(baseExpr);
    }

    public boolean isClassJoin() {
        return baseExpr instanceof IsClassExpr;
    }
}
