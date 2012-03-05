package platform.server.data.query;

import platform.base.QuickSet;
import platform.base.Result;
import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.InnerExpr;
import platform.server.data.expr.NotNullExpr;
import platform.server.data.expr.NotNullExprSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

import java.util.Collections;
import java.util.Map;

public class ExprJoin extends AbstractOuterContext<ExprJoin> implements WhereJoin<Integer, ExprJoin> {

    private final BaseExpr baseExpr;
    private final Stat stat;

    @Override
    public String toString() {
        return baseExpr + " - " + stat.toString();
    }

    protected Stat getStat() {
        return stat;
    }

    public ExprJoin(BaseExpr baseExpr, Stat stat) {
        this.baseExpr = baseExpr;
        this.stat = stat;

//        assert !baseExpr.hasOr();
    }

    public NotNullExprSet getExprFollows(boolean recursive) {
        return InnerExpr.getExprFollows(this, recursive);
    }

    @Override
    public QuickSet<OuterContext> calculateOuterDepends() {
        return new QuickSet<OuterContext>(baseExpr);
    }

    public Map<Integer, BaseExpr> getJoins() {
        return Collections.singletonMap(0, (BaseExpr)baseExpr);
    }

    public StatKeys<Integer> getStatKeys(KeyStat keyStat) {
        return new StatKeys<Integer>(Collections.singleton(0), getStat());
    }

    protected int hash(HashContext hashContext) {
        return 31 * (31 * baseExpr.hashOuter(hashContext) + stat.hashCode()) + 5;
    }

    protected ExprJoin translate(MapTranslate translator) {
        return new ExprJoin(baseExpr.translateOuter(translator), stat);
    }

    public boolean twins(TwinImmutableInterface o) {
        return baseExpr.equals(((ExprJoin)o).baseExpr) && stat.equals(((ExprJoin)o).stat);
    }

    public InnerJoins getJoinFollows(Result<Map<InnerJoin, Where>> upWheres) {
        return InnerExpr.getFollowJoins(this, upWheres);
    }

    public InnerJoins getInnerJoins() {
        InnerJoins result = new InnerJoins();
        NotNullExprSet notNullExprs = baseExpr.getExprFollows(true, false);
        for(int i=0;i<notNullExprs.size;i++) {
            NotNullExpr notNullExpr = notNullExprs.get(i);
            if(notNullExpr instanceof InnerExpr)
                result = result.and(new InnerJoins(((InnerExpr)notNullExpr).getInnerJoin()));
        }
        return result;
    }
}
