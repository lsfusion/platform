package lsfusion.server.data.query.stat;

import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.InnerExpr;
import lsfusion.server.data.expr.NullableExprInterface;
import lsfusion.server.data.expr.StaticExprInterface;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.query.StatType;

public class ValueJoin extends TwinImmutableObject implements InnerBaseJoin<Object> {

    private final StaticExprInterface staticExpr;
    private ValueJoin(StaticExprInterface staticExpr) {
        this.staticExpr = staticExpr;
    }
//    public final static ValueJoin instance = new ValueJoin();
    public static ValueJoin instance(StaticExprInterface staticExpr) {
        return new ValueJoin(staticExpr);
    }

    @Override
    protected boolean calcTwins(TwinImmutableObject o) {
        return staticExpr.equals(((ValueJoin)o).staticExpr);
    }

    @Override
    public int immutableHashCode() {
        return staticExpr.hashCode();
    }

    public ImMap<Object, BaseExpr> getJoins() {
        return MapFact.EMPTY();
    }

    public StatKeys<Object> getStatKeys(KeyStat keyStat, StatType type, boolean oldMech) {
        return new StatKeys<>(Stat.ONE);
    }

    @Override
    public Cost getPushedCost(KeyStat keyStat, StatType type, Cost pushCost, Stat pushStat, ImMap<Object, Stat> pushKeys, ImMap<Object, Stat> pushNotNullKeys, ImMap<BaseExpr, Stat> pushProps, Result<ImSet<Object>> rPushedKeys, Result<ImSet<BaseExpr>> rPushedProps) {
        assert pushKeys.isEmpty();
//        assert pushProps.size() <= 1; // ниже One быть не может
        return Cost.ONE;
    }

    public ImSet<NullableExprInterface> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
        return InnerExpr.getExprFollows(this, includeInnerWithoutNotNull, recursive);
    }

    public boolean hasExprFollowsWithoutNotNull() {
        return InnerExpr.hasExprFollowsWithoutNotNull(this);
    }
}
