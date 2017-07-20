package lsfusion.server.caches;

import lsfusion.base.GlobalInteger;
import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.query.stat.*;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.ExprTranslator;
import lsfusion.server.data.type.Type;

public abstract class ParamExpr extends VariableSingleClassExpr implements InnerBaseJoin<Object> {

    public Type getType(KeyType keyType) {
        return keyType.getKeyType(this);
    }
    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        return keyStat.getKeyStat(this, forJoin);
    }

    public Expr translate(ExprTranslator translator) {
        return this;
    }

    protected ParamExpr translate(MapTranslate translator) {
        return translator.translate(this);
    }
    public ParamExpr translateOuter(MapTranslate translator) {
        return (ParamExpr) aspectTranslate(translator);
    }

    @Override
    public int immutableHashCode() {
        return System.identityHashCode(this);
    }

    protected int hash(HashContext hashContext) {
        return hashContext.keys.hash(this);
    }

    public boolean calcTwins(TwinImmutableObject obj) {
        return false;
    }

    public PropStat getStatValue(KeyStat keyStat, StatType type) {
        return PropStat.ALOT;
//        return FormulaExpr.getStatValue(this, keyStat);
    }

    public StatKeys<Object> getStatKeys(KeyStat keyStat, StatType type, boolean oldMech) {
        return new StatKeys<>(Stat.ALOT);
//        return new StatKeys<Object>(SetFact.EMPTY(), keyStat.getKeyStat(this));
    }

    @Override
    public Cost getPushedCost(KeyStat keyStat, StatType type, Cost pushCost, Stat pushStat, ImMap<Object, Stat> pushKeys, ImMap<Object, Stat> pushNotNullKeys, ImMap<BaseExpr, Stat> pushProps, Result<ImSet<Object>> rPushedKeys, Result<ImSet<BaseExpr>> rPushedProps) {
        assert pushKeys.isEmpty(); // входов нет
        assert pushProps.size() <= 1;
        if(pushProps.isEmpty())
            return Cost.ALOT;
        return new Cost(pushProps.get(this));
    }

    public InnerBaseJoin<?> getBaseJoin() {
        return this;
    }

    public ImMap<Object, BaseExpr> getJoins() {
        return MapFact.EMPTY();
    }

    @Override
    public ImSet<NullableExprInterface> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
        return InnerExpr.getExprFollows(this, includeInnerWithoutNotNull, recursive);
    }

    @Override
    public boolean hasExprFollowsWithoutNotNull() {
        return InnerExpr.hasExprFollowsWithoutNotNull(this);
    }

    protected ImSet<ParamExpr> getKeys() {
        return SetFact.singleton(this);
    }

    public abstract GlobalInteger getKeyClass();
}
