package lsfusion.server.caches;

import lsfusion.base.GlobalInteger;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.stat.InnerBaseJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.Type;

public abstract class ParamExpr extends VariableSingleClassExpr implements InnerBaseJoin<Object> {

    public Type getType(KeyType keyType) {
        return keyType.getKeyType(this);
    }
    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        return keyStat.getKeyStat(this, forJoin);
    }

    public Expr translateQuery(QueryTranslator translator) {
        return translator.translate(this);
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

    public boolean twins(TwinImmutableObject obj) {
        return false;
    }

    public PropStat getStatValue(KeyStat keyStat) {
        return PropStat.ALOT; // временный фикс, так как при других формулах
//        return FormulaExpr.getStatValue(this, keyStat);
    }

    public StatKeys<Object> getStatKeys(KeyStat keyStat) {
        return new StatKeys<Object>(SetFact.EMPTY(), Stat.ALOT);
//        return new StatKeys<Object>(SetFact.EMPTY(), keyStat.getKeyStat(this));
    }

    public InnerBaseJoin<?> getBaseJoin() {
        return this;
    }

    public ImMap<Object, BaseExpr> getJoins() {
        return MapFact.EMPTY();
    }

    @Override
    public ImSet<NotNullExpr> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
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
