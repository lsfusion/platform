package lsfusion.server.data.query.stat;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.DistinctKeys;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.translator.MapTranslate;

public class StatKeys<K> extends TwinImmutableObject {

    public final Stat rows;

    public final DistinctKeys<K> distinct;

    public StatKeys(ImSet<K> allKeys) {
        this(allKeys, Stat.MIN);
    }

    public ImSet<K> getKeys() {
        return distinct.keys();
    }

    public StatKeys(ImSet<K> allKeys, Stat stat) {
        this(stat, new DistinctKeys<K>(allKeys.toMap(stat)));
    }

    public StatKeys(Stat rows, DistinctKeys<K> distinct) {
        this.rows = rows;
        this.distinct = distinct;
        // если rows > max, то при удалении join'а может статистика уменьшится, а не увеличится
        assert distinct.isEmpty() || rows.equals(Stat.MIN) || (rows.lessEquals(distinct.getMax()) && distinct.getMaxKey().lessEquals(rows));
    }

    // по идее важно только в WhereJoins, в остальных за счет того что статистика округляется вверх по идее должен выполняться верхний assertion
    public static <K> StatKeys<K> create(Stat rows, DistinctKeys<K> distinct) {
        return new StatKeys<K>(distinct.getMax().min(rows), distinct);
    }

    public <T> StatKeys<T> mapBack(ImMap<T, K> map) {
        return new StatKeys<T>(rows, distinct.mapBack(map));
    }

    public StatKeys<K> or(StatKeys<K> stat) {
        return new StatKeys<K>(rows.or(stat.rows), distinct.or(stat.distinct));
    }

    public static <K extends Expr> int hashOuter(StatKeys<K> statKeys, HashContext hashContext) {
        return statKeys.rows.hashCode() * 31 + DistinctKeys.hashOuter(statKeys.distinct, hashContext);
    }

    public static <K extends Expr> StatKeys<K> translateOuter(StatKeys<K> statKeys, MapTranslate translator) {
        return new StatKeys<K>(statKeys.rows, DistinctKeys.translateOuter(statKeys.distinct, translator));
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return rows.equals(((StatKeys)o).rows) && distinct.equals(((StatKeys)o).distinct);
    }

    public int immutableHashCode() {
        return 31 * rows.hashCode() + distinct.hashCode();
    }

    private final static AddValue<Object, StatKeys<Object>> addOr = new SymmAddValue<Object, StatKeys<Object>>() {
        public StatKeys<Object> addValue(Object key, StatKeys<Object> prevValue, StatKeys<Object> newValue) {
            return prevValue.or(newValue);
        }
    };
    public static <M, K> AddValue<M, StatKeys<K>> addOr() {
        return BaseUtils.immutableCast(addOr);
    }
}

