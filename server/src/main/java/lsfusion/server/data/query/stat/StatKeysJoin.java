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
import lsfusion.server.data.expr.NotNullExprInterface;
import lsfusion.server.data.query.InnerJoin;
import lsfusion.server.data.query.InnerJoins;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.where.Where;

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

    public InnerJoins getJoinFollows(Result<ImMap<InnerJoin, Where>> upWheres, Result<ImSet<UnionJoin>> unionJoins) { // все равно использует getExprFollows
        return InnerExpr.getJoinFollows(this, upWheres, unionJoins);
    }

    public ImSet<NotNullExprInterface> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
        return InnerExpr.getExprFollows(this, includeInnerWithoutNotNull, recursive);
    }

    public ImMap<K, BaseExpr> getJoins() {
        return BaseUtils.immutableCast(stat.getKeys().toMap());
    }

    public StatKeys<K> getStatKeys(KeyStat keyStat) {
        return stat;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return stat.equals(((StatKeysJoin<K>) o).stat);
    }

    public InnerJoins getInnerJoins() {
        throw new UnsupportedOperationException();
    }
}
