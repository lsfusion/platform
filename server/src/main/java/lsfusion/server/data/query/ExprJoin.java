package lsfusion.server.data.query;

import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.query.stat.UnionJoin;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.where.Where;

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

    public boolean calcTwins(TwinImmutableObject o) {
        return baseExpr.equals(((ExprJoin) o).baseExpr);
    }

    public InnerJoins getJoinFollows(Result<ImMap<InnerJoin, Where>> upWheres, Result<ImSet<UnionJoin>> unionJoins) { // все равно использует getExprFollows
        return InnerExpr.getFollowJoins(this, upWheres, unionJoins);
    }

    public ImSet<NotNullExprInterface> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
        return InnerExpr.getExprFollows(this, includeInnerWithoutNotNull, recursive);
    }

    public ImMap<Integer, BaseExpr> getJoins() {
        return MapFact.singleton(0, (BaseExpr) baseExpr);
    }

    public static InnerJoins getInnerJoins(BaseExpr baseExpr) {
        InnerJoins result = InnerJoins.EMPTY;
        ImSet<InnerExpr> innerExprs = NotNullExpr.getInnerExprs(baseExpr.getExprFollows(true, NotNullExpr.INNERJOINS, false), null);
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

    public boolean givesNoKeys() {
        return baseExpr instanceof KeyExpr;
    }
}
