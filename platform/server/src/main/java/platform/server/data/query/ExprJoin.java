package platform.server.data.query;

import platform.base.Result;
import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.InnerExpr;
import platform.server.data.expr.IsClassExpr;
import platform.server.data.expr.NotNullExpr;
import platform.server.data.query.stat.UnionJoin;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.where.Where;

public abstract class ExprJoin<T extends ExprJoin<T>> extends AbstractOuterContext<T> implements WhereJoin<Integer, T> {

    protected final BaseExpr baseExpr;

    public ExprJoin(BaseExpr baseExpr) {
        this.baseExpr = baseExpr;
    }

    @Override
    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.<OuterContext>singleton(baseExpr);
    }

    protected int hash(HashContext hashContext) {
        return baseExpr.hashOuter(hashContext);
    }

    public boolean twins(TwinImmutableObject o) {
        return baseExpr.equals(((ExprJoin) o).baseExpr);
    }

    public InnerJoins getJoinFollows(Result<ImMap<InnerJoin, Where>> upWheres, Result<ImSet<UnionJoin>> unionJoins) { // все равно использует getExprFollows
        return InnerExpr.getFollowJoins(this, upWheres, unionJoins);
    }

    public ImSet<NotNullExpr> getExprFollows(boolean recursive) {
        return InnerExpr.getExprFollows(this, recursive);
    }

    public ImMap<Integer, BaseExpr> getJoins() {
        return MapFact.singleton(0, (BaseExpr) baseExpr);
    }

    public static InnerJoins getInnerJoins(BaseExpr baseExpr) {
        InnerJoins result = new InnerJoins();
        ImSet<InnerExpr> innerExprs = NotNullExpr.getInnerExprs(baseExpr.getExprFollows(true, false), null);
        for(int i=0,size=innerExprs.size();i<size;i++)
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
