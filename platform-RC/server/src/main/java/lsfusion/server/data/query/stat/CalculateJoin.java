package lsfusion.server.data.query.stat;

import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityQuickLazy;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.InnerExpr;
import lsfusion.server.data.expr.NullableExprInterface;
import lsfusion.server.data.expr.query.DistinctKeys;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.query.StatType;

public abstract class CalculateJoin<K> extends TwinImmutableObject implements InnerBaseJoin<K> {

    public StatKeys<K> getStatKeys(final KeyStat keyStat, StatType type, boolean oldMech) {
        return getCalcStatKeys(keyStat);
    }

    private StatKeys<K> getCalcStatKeys(final KeyStat keyStat) {
        Stat totalStat = Stat.ONE;
        ImMap<K, BaseExpr> joins = WhereJoins.getJoinsForStat(this);
        ImMap<K, Stat> distinct = joins.mapValues(new GetValue<Stat, BaseExpr>() {
            public Stat getMapValue(BaseExpr value) {
                return value.getTypeStat(keyStat, true);
            }});
        for(Stat stat : distinct.valueIt())
            totalStat = totalStat.mult(stat);
        return new StatKeys<>(totalStat, new DistinctKeys<>(distinct)); // , Cost.CALC
    }

    @Override
    public Cost getPushedCost(KeyStat keyStat, StatType type, Cost pushCost, Stat pushStat, ImMap<K, Stat> pushKeys, ImMap<K, Stat> pushNotNullKeys, ImMap<BaseExpr, Stat> pushProps, Result<ImSet<K>> rPushedKeys, Result<ImSet<BaseExpr>> rPushedProps) {
//        StatKeys<K> statKeys = getCalcStatKeys(keyStat);
        if(pushKeys.size() < getJoins().size()) // не все ключи есть, запретим выбирать
            return Cost.ALOT;
        return pushCost; // иначе cost равен cost'у контекста
    }

    public ImSet<NullableExprInterface> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
        return InnerExpr.getExprFollows(this, includeInnerWithoutNotNull, recursive);
    }

    public boolean hasExprFollowsWithoutNotNull() {
        return InnerExpr.hasExprFollowsWithoutNotNull(this);
    }
}
