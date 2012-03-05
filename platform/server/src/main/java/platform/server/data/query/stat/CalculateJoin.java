package platform.server.data.query.stat;

import platform.base.TwinImmutableInterface;
import platform.base.TwinImmutableObject;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.InnerExpr;
import platform.server.data.expr.NotNullExprSet;
import platform.server.data.expr.query.DistinctKeys;
import platform.server.data.expr.query.Stat;

import java.util.Map;

public class CalculateJoin<K> extends TwinImmutableObject implements InnerBaseJoin<K> {

    public final Map<K, BaseExpr> params;

    public CalculateJoin(Map<K, BaseExpr> params) {
        this.params = params;
    }

    public Map<K, BaseExpr> getJoins() {
        return params;
    }

    public StatKeys<K> getStatKeys(KeyStat keyStat) {
        Stat totalStat = Stat.ONE;
        DistinctKeys<K> distinct = new DistinctKeys<K>();
        for(Map.Entry<K, BaseExpr> param : getJoins().entrySet()) {
            Stat stat = param.getValue().getTypeStat(keyStat);
            distinct.add(param.getKey(), stat);
            totalStat = totalStat.mult(stat);
        }
        return new StatKeys<K>(totalStat, distinct);
    }

    public NotNullExprSet getExprFollows(boolean recursive) {
        return InnerExpr.getExprFollows(this, recursive);
    }

    public boolean twins(TwinImmutableInterface o) {
        return params.equals(((CalculateJoin)o).params);
    }

    public int immutableHashCode() {
        return params.hashCode();
    }
}
