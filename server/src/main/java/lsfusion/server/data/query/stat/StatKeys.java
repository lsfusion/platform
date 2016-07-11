package lsfusion.server.data.query.stat;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.DistinctKeys;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.translator.MapTranslate;

public class StatKeys<K> extends TwinImmutableObject {

    public final Stat rows;

    // пытаться использовать информацию по индексам, для определения сложности выполнения WhereJoins
    // проблема в том что зависит от push down в QueryJoin, а на получении статистики это тяжело сделать так как, алгоритм сложный и зависит от многих факторов (плюс все равно LEFT JOIN'ы не учитываются)
    // поэтому пока будем исходить из того что все поля "проиндексированы"
//    public final ExecCost cost;

    public final DistinctKeys<K> distinct;

    public StatKeys(ImSet<K> allKeys) { // конструктор для or, по сути нужно min'ы передавать
        this(allKeys, Stat.MIN); //, ExecCost.MIN
    }

    public ImSet<K> getKeys() {
        return distinct.keys();
    }

//    public StatKeys(ImSet<K> allKeys, Stat stat) {
//        this(allKeys, stat); // ExecCost.CALC
//    }

    public StatKeys(ImSet<K> allKeys, Stat stat) { // , ExecCost cost
        this(stat, new DistinctKeys<>(allKeys.toMap(stat))); // , cost
    }

    public StatKeys(Stat rows, DistinctKeys<K> distinct) { // , ExecCost cost
        this.rows = rows;
        this.distinct = distinct;
//        this.cost = cost;
        // если rows > max, то при удалении join'а может статистика уменьшится, а не увеличится
        assert distinct.isEmpty() || rows.equals(Stat.MIN) || (rows.lessEquals(distinct.getMax()) && distinct.getMaxKey().lessEquals(rows));
    }

    // по идее не важно (в отличии от WhereJoins), в остальных за счет того что статистика округляется вверх по идее должен выполняться assertion из конструктора
    public static <K> StatKeys<K> createForTable(Stat rows, DistinctKeys<K> distinct) {
        return create(distinct.getMax().min(rows), distinct); // , new ExecCost(rows) тут в общем то и execCount можно вытянуть, но с точки зрения кэширования очень опасно (только для не SessionTable делать ???)
    }

    public static <K> StatKeys<K> create(Stat rows, DistinctKeys<K> distinct) { //, ExecCost cost
        return new StatKeys<>(distinct.getMax().min(rows), distinct); // , cost
    }

    public StatKeys<K> decrease(final Stat dec) {
        return new StatKeys<>(dec, new DistinctKeys<>(distinct.mapValues(new GetValue<Stat, Stat>() {
            public Stat getMapValue(Stat value) {
                return value.min(dec);
            }
        })));
    }


    public <T> StatKeys<T> mapBack(ImMap<T, K> map) {
        return new StatKeys<>(rows, distinct.mapBack(map)); // , cost
    }

    public StatKeys<K> or(StatKeys<K> stat) {
        return new StatKeys<>(rows.or(stat.rows), distinct.or(stat.distinct)); // , cost.or(stat.cost)
    }

    public static <K extends Expr> int hashOuter(StatKeys<K> statKeys, HashContext hashContext) {
        return statKeys.rows.hashCode() * 31 + DistinctKeys.hashOuter(statKeys.distinct, hashContext); //  + 31 * 31 * statKeys.cost.hashCode()
    }

    public static <K extends Expr> StatKeys<K> translateOuter(StatKeys<K> statKeys, MapTranslate translator) {
        return new StatKeys<>(statKeys.rows, DistinctKeys.translateOuter(statKeys.distinct, translator)); // , statKeys.cost
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return rows.equals(((StatKeys)o).rows) && distinct.equals(((StatKeys)o).distinct); //  && cost.equals(((StatKeys)o).cost)
    }

    public int immutableHashCode() {
        return 31 * rows.hashCode() + distinct.hashCode(); //  + 31 * 31 * cost.hashCode()
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

