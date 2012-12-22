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
import platform.server.data.expr.NotNullExpr;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.query.stat.UnionJoin;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

public class ExprEqualsJoin extends AbstractOuterContext<ExprEqualsJoin> implements WhereJoin<Integer, ExprEqualsJoin> {

    public BaseExpr expr1;
    public BaseExpr expr2;

    public ExprEqualsJoin(BaseExpr expr1, BaseExpr expr2) {
        this.expr1 = expr1;
        this.expr2 = expr2;
    }

    public int hash(HashContext hashContext) {
        return 31 * expr1.hashOuter(hashContext) + expr2.hashOuter(hashContext);
    }

    protected ExprEqualsJoin translate(MapTranslate translator) {
        return new ExprEqualsJoin(expr1.translateOuter(translator), expr2.translateOuter(translator));
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return SetFact.<OuterContext>toSet(expr1, expr2);
    }

    public InnerJoins getInnerJoins() {
        return ExprJoin.getInnerJoins(expr1).and(ExprJoin.getInnerJoins(expr2));
    }

    public InnerJoins getJoinFollows(Result<ImMap<InnerJoin, Where>> upWheres, Result<ImSet<UnionJoin>> unionJoins) {
        return InnerExpr.getFollowJoins(this, upWheres, unionJoins);
    }

    public ImSet<NotNullExpr> getExprFollows(boolean recursive) {
        return InnerExpr.getExprFollows(this, recursive);
    }

    public ImMap<Integer, BaseExpr> getJoins() {
        return MapFact.toMap(0, expr1, 1, expr2);
    }

    public StatKeys<Integer> getStatKeys(KeyStat keyStat) {
        return new StatKeys<Integer>(SetFact.toExclSet(0, 1), expr1.getTypeStat(keyStat).min(expr2.getTypeStat(keyStat)));
    }

    public boolean twins(TwinImmutableObject o) {
        return expr1.equals(((ExprEqualsJoin)o).expr1) && expr2.equals(((ExprEqualsJoin)o).expr2);
    }
}
