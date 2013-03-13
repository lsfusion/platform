package platform.server.data.expr;

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

public class KeyExpr extends VariableClassExpr implements InnerBaseJoin<Object> {

    private static final GetValue<KeyExpr, Object> genStringKeys = new GetValue<KeyExpr, Object>() {
        public KeyExpr getMapValue(Object value) {
            return new KeyExpr(value.toString());
        }
    };
    private static final GetIndex<KeyExpr> genIndexKeys = new GetIndex<KeyExpr>() {
        public KeyExpr getMapValue(int i) {
            return new KeyExpr(i);
        }
    };
    public static <T> ImRevMap<T, KeyExpr> getMapKeys(ImSet<T> objects) {
        if (SystemProperties.isDebug)
            return objects.mapRevValues((GetValue<KeyExpr, T>) genStringKeys);

        return objects.mapRevValues(genIndexKeys);
    }

    final Object name;
    @Override
    public String toString() {
        return name.toString();
    }

    public KeyExpr(String name) {
        this.name = name;
    }

    public KeyExpr(int id) {
        this.name = id;
    }

    public String getSource(CompileSource compile) {
        String source = compile.getSource(this);
        assert source!=null;
        return source;
    }

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
    }

    public Type getType(KeyType keyType) {
        return keyType.getKeyType(this);
    }
    public Stat getTypeStat(KeyStat keyStat) {
        return keyStat.getKeyStat(this);
    }

    public Expr translateQuery(QueryTranslator translator) {
        return translator.translate(this);
    }

    protected KeyExpr translate(MapTranslate translator) {
        return translator.translate(this);
    }
    public KeyExpr translateOuter(MapTranslate translator) {
        return (KeyExpr) aspectTranslate(translator);
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

    @Override
    protected ImSet<KeyExpr> getKeys() {
        return SetFact.singleton(this);
    }

    public boolean isTableIndexed() {
        return true;
    }
}
