package platform.server.data.expr;

import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.MapWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class KeyExpr extends VariableClassExpr implements InnerBaseJoin<Object> {

    public static <T> Map<T, KeyExpr> getMapKeys(Collection<T> objects) {
        Map<T,KeyExpr> result = new HashMap<T, KeyExpr>();
        for(T object : objects)
            result.put(object,new KeyExpr(object.toString()));
        return result;
    }

    final String name;
    @Override
    public String toString() {
        return name;
    }

    public KeyExpr(String name) {
        this.name = name;
    }

    public String getSource(CompileSource compile) {
        assert compile.keySelect.containsKey(this);
        return compile.keySelect.get(this);
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
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

    public boolean twins(TwinImmutableInterface obj) {
        return false;
    }

    public Stat getStatValue(KeyStat keyStat) {
        return FormulaExpr.getStatValue(this, keyStat);
    }

    public StatKeys<Object> getStatKeys(KeyStat keyStat) {
        return new StatKeys<Object>(new HashSet<Object>(), keyStat.getKeyStat(this));
    }

    public InnerBaseJoin<?> getBaseJoin() {
        return this;
    }

    public Map<Object, BaseExpr> getJoins() {
        return new HashMap<Object, BaseExpr>();
    }

    public NotNullExprSet getExprFollows(boolean recursive) {
        return InnerExpr.getExprFollows(this, recursive);
    }

    @Override
    protected QuickSet<KeyExpr> getKeys() {
        return new QuickSet<KeyExpr>(this);
    }
}
