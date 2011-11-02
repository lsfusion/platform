package platform.server.data.query;

import platform.base.Result;
import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.InnerExpr;
import platform.server.data.expr.InnerExprSet;
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

        assert !baseExpr.isOr();
    }

    public InnerExprSet getExprFollows(boolean recursive) {
        return InnerExpr.getExprFollows(this, recursive);
    }

    public SourceJoin[] getEnum() {
        return new SourceJoin[]{baseExpr};
    }

    public Map<Integer, BaseExpr> getJoins() {
        return Collections.singletonMap(0, (BaseExpr)baseExpr);
    }

    public StatKeys<Integer> getStatKeys(KeyStat keyStat) {
        return new StatKeys<Integer>(Collections.singleton(0), getStat());
    }

    public int hashOuter(HashContext hashContext) {
        return 31 * (31 * baseExpr.hashOuter(hashContext) + stat.hashCode()) + 5;
    }

    public ExprJoin translateOuter(MapTranslate translator) {
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
        InnerExprSet innerExprs = baseExpr.getExprFollows(true, false);
        for(int i=0;i<innerExprs.size;i++)
            result = result.and(new InnerJoins(innerExprs.get(i).getInnerJoin()));
        return result;
    }
}
