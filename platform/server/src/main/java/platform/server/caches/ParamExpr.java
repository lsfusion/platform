package platform.server.caches;

import platform.base.GlobalInteger;
import platform.base.GlobalObject;
import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.SystemProperties;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.*;
import platform.server.data.expr.query.PropStat;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

public abstract class ParamExpr extends VariableSingleClassExpr implements InnerBaseJoin<Object> {

    public Type getType(KeyType keyType) {
        return keyType.getKeyType(this);
    }
    public Stat getTypeStat(KeyStat keyStat) {
        return keyStat.getKeyStat(this);
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

    public ImSet<NotNullExpr> getExprFollows(boolean recursive) {
        return InnerExpr.getExprFollows(this, recursive);
    }

    protected ImSet<ParamExpr> getKeys() {
        return SetFact.singleton(this);
    }

    public abstract GlobalInteger getKeyClass();
}
