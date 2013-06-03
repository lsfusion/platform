package lsfusion.server.data.query.stat;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.InnerExpr;
import lsfusion.server.data.expr.NotNullExpr;
import lsfusion.server.data.expr.query.DistinctKeys;
import lsfusion.server.data.expr.query.Stat;

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
