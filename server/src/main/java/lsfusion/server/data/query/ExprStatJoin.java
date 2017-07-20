package lsfusion.server.data.query;

import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.query.stat.Cost;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.translator.MapTranslate;

public class ExprStatJoin extends ExprJoin<ExprStatJoin> {

    private final Stat stat;
    private final InnerJoins valueJoins;
    public final boolean notNull; // чисто для SQLSyntax.hasNotNullIndexProblem

    @Override
    public String toString() {
        return baseExpr + " - " + stat.toString() + " " + valueJoins + " " + notNull;
    }

    @Override
    public StatKeys<Integer> getStatKeys(KeyStat keyStat, StatType type, boolean oldMech) {
        return new StatKeys<>(SetFact.singleton(0), stat);
    }

    @Override
    public Cost getPushedCost(KeyStat keyStat, StatType type, Cost pushCost, Stat pushStat, ImMap<Integer, Stat> pushKeys, ImMap<Integer, Stat> pushNotNullKeys, ImMap<BaseExpr, Stat> pushProps, Result<ImSet<Integer>> rPushedKeys, Result<ImSet<BaseExpr>> rPushedProps) {
        assert pushProps.isEmpty();
        assert pushKeys.size() <= 1;
        if(pushKeys.isEmpty())
            return Cost.ALOT;
        return new Cost(stat).min(pushCost);
    }

    public ExprStatJoin(BaseExpr baseExpr, Stat stat, BaseExpr valueExpr) {
        this(baseExpr, stat, getInnerJoins(valueExpr), false);
        assert valueExpr.isValue();
        assert !givesNoKeys(); //  || Settings.get().isUseCommonWhere()calculateKeyEquals по идее должен устранить keyExpr = value (другое дело что groupNotJoinsWheres мог бы дать эту ситуацию, но сейчас там другая реализация getSymmetricGreaterWhere)
        // не выполняется из-за getCommonWhere, так как он включается не в calculateOrWhere (туда включается более общий Where), а в keyEquals, groupJoinsWheres, classWhere (может их исключить оттуда) ??
    }

    public ExprStatJoin(BaseExpr baseExpr, Stat stat) {
        this(baseExpr, stat, false);

    }
    public ExprStatJoin(BaseExpr baseExpr, Stat stat, boolean notNull) {
        this(baseExpr, stat, InnerJoins.EMPTY, notNull);
    }

    // adjust constructor
    public ExprStatJoin(BaseExpr baseExpr, Stat statValue, Stat desiredJoinStat, Stat joinStat, boolean notNull) {
        this(baseExpr, statValue.mult(desiredJoinStat).div(joinStat), notNull);
    }

    public boolean depends(InnerJoin join) {
        return valueJoins.containsAll(join);
    }

    public ExprStatJoin(BaseExpr baseExpr, Stat stat, InnerJoins valueJoins, boolean notNull) {
        super(baseExpr);

        this.stat = stat;
        this.valueJoins = valueJoins;
        this.notNull = notNull;
    }

    @Override
    public InnerJoins getInnerJoins() {
        return super.getInnerJoins().and(valueJoins);
    }

    protected int hash(HashContext hashContext) {
        return 31 * (31 * super.hash(hashContext) + stat.hashCode()) + (notNull ? 1 : 0) + 5;
    }

    protected ExprStatJoin translate(MapTranslate translator) {
        return new ExprStatJoin(baseExpr.translateOuter(translator), stat, valueJoins.translate(translator.mapValues()), notNull);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && stat.equals(((ExprStatJoin)o).stat) && valueJoins.equals(((ExprStatJoin)o).valueJoins) && notNull == (((ExprStatJoin)o).notNull);
    }
}
