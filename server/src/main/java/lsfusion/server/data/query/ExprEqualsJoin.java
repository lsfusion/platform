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
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.InnerExpr;
import lsfusion.server.data.expr.NotNullExpr;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.query.stat.UnionJoin;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.where.Where;

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

    public ImSet<NotNullExpr> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
        return InnerExpr.getExprFollows(this, includeInnerWithoutNotNull, recursive);
    }

    public ImMap<Integer, BaseExpr> getJoins() {
        return MapFact.toMap(0, expr1, 1, expr2);
    }

    public StatKeys<Integer> getStatKeys(KeyStat keyStat) { // тут по идее forJoin и true и false подойдут
        return new StatKeys<Integer>(SetFact.toExclSet(0, 1), expr1.getTypeStat(keyStat, false).min(expr2.getTypeStat(keyStat, false)));
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return expr1.equals(((ExprEqualsJoin)o).expr1) && expr2.equals(((ExprEqualsJoin)o).expr2);
    }
}
