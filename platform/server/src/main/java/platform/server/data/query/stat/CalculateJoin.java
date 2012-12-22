package platform.server.data.query.stat;

import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.InnerExpr;
import platform.server.data.expr.NotNullExpr;
import platform.server.data.expr.query.DistinctKeys;
import platform.server.data.expr.query.Stat;

public abstract class CalculateJoin<K> extends TwinImmutableObject implements InnerBaseJoin<K> {

    public StatKeys<K> getStatKeys(final KeyStat keyStat) {
        Stat totalStat = Stat.ONE;
        ImMap<K, BaseExpr> joins = getJoins();
        ImMap<K, Stat> distinct = joins.mapValues(new GetValue<Stat, BaseExpr>() {
            public Stat getMapValue(BaseExpr value) {
                return value.getTypeStat(keyStat);
            }});
        for(Stat stat : distinct.valueIt())
            totalStat = totalStat.mult(stat);
        return new StatKeys<K>(totalStat, new DistinctKeys<K>(distinct));
    }

    public ImSet<NotNullExpr> getExprFollows(boolean recursive) {
        return InnerExpr.getExprFollows(this, recursive);
    }
}
