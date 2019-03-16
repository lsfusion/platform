package lsfusion.server.data.expr.join.select;

import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.classes.IsClassExpr;
import lsfusion.server.data.expr.inner.InnerExpr;
import lsfusion.server.data.expr.join.inner.InnerJoin;
import lsfusion.server.data.expr.join.inner.InnerJoins;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.compile.where.UpWheres;
import lsfusion.server.data.expr.join.base.UnionJoin;
import lsfusion.server.data.expr.join.where.WhereJoin;

public abstract class ExprJoin<T extends ExprJoin<T>> extends AbstractOuterContext<T> implements WhereJoin<Integer, T> {

    protected final BaseExpr baseExpr;

    public ExprJoin(BaseExpr baseExpr) {
        this.baseExpr = baseExpr;
    }

    @Override
    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.<OuterContext>singleton(baseExpr);
    }

    public int hash(HashContext hashContext) {
        return baseExpr.hashOuter(hashContext);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return baseExpr.equals(((ExprJoin) o).baseExpr);
    }

    public InnerJoins getJoinFollows(Result<UpWheres<InnerJoin>> upWheres, MSet<UnionJoin> mUnionJoins) { // все равно использует getExprFollows
        return InnerExpr.getJoinFollows(this, upWheres, mUnionJoins);
    }

    public ImSet<NullableExprInterface> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
        return InnerExpr.getExprFollows(this, includeInnerWithoutNotNull, recursive);
    }

    // может ли при замене выражения на ключ сохранить информацию о join'е (или все же неявно использует семантику выражения) - хак в определенной степени
    public boolean canBeKeyJoined() {
        return true;
    }

    public ImMap<Integer, BaseExpr> getJoins() {
        return MapFact.singleton(0, (BaseExpr) baseExpr);
    }

    public static InnerJoins getInnerJoins(BaseExpr baseExpr) {
        InnerJoins result = InnerJoins.EMPTY;
        ImSet<InnerExpr> innerExprs = NullableExpr.getInnerExprs(baseExpr.getExprFollows(true, NullableExpr.INNERJOINS, false), null);
        for (int i = 0, size = innerExprs.size(); i < size; i++)
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

    public KeyExpr getKeyExpr() {
        if(baseExpr instanceof KeyExpr)
            // assert !not
            return (KeyExpr) baseExpr;
        return null;
    }
}