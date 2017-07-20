package lsfusion.server.data.query.stat;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.InnerExpr;
import lsfusion.server.data.expr.NullableExprInterface;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.query.InnerJoin;
import lsfusion.server.data.query.InnerJoins;
import lsfusion.server.data.query.innerjoins.UpWheres;
import lsfusion.server.data.translator.MapTranslate;

public class StatKeysJoin<K extends BaseExpr> extends AbstractOuterContext<StatKeysJoin<K>> implements WhereJoin<K, StatKeysJoin<K>> {
    
    private final StatKeys<K> stat;

    public StatKeysJoin(StatKeys<K> stat) {
        this.stat = stat;
    }

    protected ImSet<OuterContext> calculateOuterDepends() {
        return BaseUtils.immutableCast(stat.getKeys());
    }

    protected StatKeysJoin<K> translate(MapTranslate translator) {
        return new StatKeysJoin<>(StatKeys.translateOuter(stat, translator));
    }

    protected int hash(HashContext hash) {
        return StatKeys.hashOuter(stat, hash);
    }

    public InnerJoins getJoinFollows(Result<UpWheres<InnerJoin>> upWheres, Result<ImSet<UnionJoin>> unionJoins) { // все равно использует getExprFollows
        return InnerExpr.getJoinFollows(this, upWheres, unionJoins);
    }

    public ImSet<NullableExprInterface> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
        return InnerExpr.getExprFollows(this, includeInnerWithoutNotNull, recursive);
    }

    public ImMap<K, BaseExpr> getJoins() {
        return BaseUtils.immutableCast(stat.getKeys().toMap());
    }

    public StatKeys<K> getStatKeys(KeyStat keyStat, StatType type, boolean oldMech) {
        return stat;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return stat.equals(((StatKeysJoin<K>) o).stat);
    }

    @Override
    public Cost getPushedCost(KeyStat keyStat, StatType type, Cost pushCost, Stat pushStat, ImMap<K, Stat> pushKeys, ImMap<K, Stat> pushNotNullKeys, ImMap<BaseExpr, Stat> pushProps, Result<ImSet<K>> rPushedKeys, Result<ImSet<BaseExpr>> rPushedProps) {
        return stat.getCost(); // считаем, что не редуцируется
    }

    public InnerJoins getInnerJoins() {
        throw new UnsupportedOperationException();
    }
}
