package lsfusion.server.data.query.stat;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.query.DistinctKeys;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.translator.MapTranslate;

public class StatKeys<K> extends TwinImmutableObject {

    private final Cost cost;

    public static <K> StatKeys<K> NOPUSH() {
        return null;
    }

    private final Stat stat;
    private final DistinctKeys<K> distinct;

    public StatKeys<K> toRevMap(ImMap<K, BaseExpr> innerOuter, Result<ImRevMap<K, BaseExpr>> rRevInnerOuter) {
        // преобразуем joins в reverse map, пока делаем просто и выбираем минимум innerKeys (то есть с меньшим числом разновидностей)
        MMap<BaseExpr, K> mRevOuterInner = MapFact.mMap(MapFact.<BaseExpr, K>override());
        for(int i=0,size=innerOuter.size();i<size;i++) {
            K inner = innerOuter.getKey(i);
            BaseExpr outer = innerOuter.getValue(i);
            K revInner = mRevOuterInner.get(outer);
            if(revInner == null || distinct.get(inner).less(distinct.get(revInner)))
                mRevOuterInner.add(outer, inner);
        }
        ImRevMap<K, BaseExpr> revInnerOuter = mRevOuterInner.immutable().toRevExclMap().reverse();
        rRevInnerOuter.set(revInnerOuter);
        if(revInnerOuter.size() != innerOuter.size())
            return new StatKeys<>(cost, stat, new DistinctKeys<>(distinct.filterIncl(revInnerOuter.keys())));
        return this;
    }

    public Stat getRows() {
        return stat;
    }

    public Cost getCost() {
        return cost;
    }

    public DistinctKeys<K> getDistinct() {
        return distinct;
    }

    public Stat getDistinct(K key) {
        return distinct.get(key);
    }

    // пытаться использовать информацию по индексам, для определения сложности выполнения WhereJoins
    // проблема в том что зависит от push down в QueryJoin, а на получении статистики это тяжело сделать так как, алгоритм сложный и зависит от многих факторов (плюс все равно LEFT JOIN'ы не учитываются)
    // поэтому пока будем исходить из того что все поля "проиндексированы"
//    public final Cost cost;

    public static StatKeys<KeyField> create(TableStatKeys tableStatKeys) {
        return new StatKeys<>(tableStatKeys.getRows(), tableStatKeys.getDistinct()); // assert что все ключи индексированы
    }

    private StatKeys(ImSet<K> allKeys) { // конструктор для or, по сути нужно min'ы передавать
        this(allKeys, Stat.MIN); //, Cost.MIN
    }
    public static <K, V> StatKeys<K> or(Iterable<V> col, GetValue<StatKeys<K>, V> getter, ImSet<K> allKeys) {
        StatKeys<K> result = new StatKeys<>(allKeys);
        for(V value : col)
            result = result.or(getter.getMapValue(value));
        return result;
    }

    public ImSet<K> getKeys() {
        return distinct.keys();
    }

//    public StatKeys(ImSet<K> allKeys, Stat stat) {
//        this(allKeys, stat); // Cost.CALC
//    }

    public StatKeys(Stat stat) { // , Cost cost
        this(SetFact.<K>EMPTY(), stat); // , cost
    }

    public StatKeys(ImSet<K> allKeys, Stat stat) { // , Cost cost
        this(stat, new DistinctKeys<>(allKeys.toMap(stat))); // , cost
    }

    // calculate constructor : a * b, a = b, a = 5, 5, key, у всех cost = stat (так как costreduce полный у Keys / Value)
    public StatKeys(Stat rows, DistinctKeys<K> distinct) { // , Cost cost
        this(new Cost(rows), rows, distinct);
    }
    public StatKeys(Cost cost, Stat stat, DistinctKeys<K> distinct) {
        this(cost, stat, distinct, true);
    }
    public StatKeys(Cost cost, Stat stat, DistinctKeys<K> distinct, boolean checkCost) {
        this.cost = cost;
        this.stat = stat;
        this.distinct = distinct;
//        this.cost = cost;
        // если rows > max, то при удалении join'а может статистика уменьшится, а не увеличится
        assert distinct.isEmpty() || stat.equals(Stat.MIN) || (stat.lessEquals(distinct.getMax()) && distinct.getMaxKey().lessEquals(stat));
        assert !checkCost || stat.lessEquals(cost.rows);
    }

    public <T> StatKeys<T> mapBack(ImMap<T, K> map) {
        return new StatKeys<>(cost, stat, distinct.mapBack(map)); // , cost
    }

    public StatKeys<K> or(StatKeys<K> statKeys) {
        return new StatKeys<>(cost.or(statKeys.cost), stat.or(statKeys.stat), distinct.or(statKeys.distinct)); // , cost.or(stat.cost)
    }

    public static <K extends Expr> int hashOuter(ImMap<StatType, StatKeys<K>> statKeys, HashContext hashContext) {
        int hash = 0;
        for(int i=0,size=statKeys.size();i<size;i++)
            hash += statKeys.getKey(i).hashCode() ^ hashOuter(statKeys.getValue(i), hashContext);
        return hash;
    }

    public static <K extends Expr> ImMap<StatType, StatKeys<K>> translateOuter(ImMap<StatType, StatKeys<K>> statKeys, final MapTranslate translator) {
        return statKeys.mapValues(new GetValue<StatKeys<K>, StatKeys<K>>() {
            public StatKeys<K> getMapValue(StatKeys<K> value) {
                return translateOuter(value, translator);
            }});
    }

    public static <K extends Expr> int hashOuter(StatKeys<K> statKeys, HashContext hashContext) {
        return 31 * (statKeys.cost.hashCode() * 31 + DistinctKeys.hashOuter(statKeys.distinct, hashContext)) + statKeys.stat.hashCode(); //  + 31 * 31 * statKeys.cost.hashCode()
    }

    public static <K extends Expr> StatKeys<K> translateOuter(StatKeys<K> statKeys, MapTranslate translator) {
        return new StatKeys<>(statKeys.cost, statKeys.stat, DistinctKeys.translateOuter(statKeys.distinct, translator)); // , statKeys.cost
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return cost.equals(((StatKeys)o).cost) && stat.equals(((StatKeys)o).stat) && distinct.equals(((StatKeys)o).distinct); //  && cost.equals(((StatKeys)o).cost)
    }

    public int immutableHashCode() {
        return 31 * (cost.hashCode() * 31 + distinct.hashCode()) + stat.hashCode(); //  + 31 * 31 * cost.hashCode()
    }

    private final static AddValue<Object, StatKeys<Object>> addOr = new SymmAddValue<Object, StatKeys<Object>>() {
        public StatKeys<Object> addValue(Object key, StatKeys<Object> prevValue, StatKeys<Object> newValue) {
            return prevValue.or(newValue);
        }
    };
    public static <M, K> AddValue<M, StatKeys<K>> addOr() {
        return BaseUtils.immutableCast(addOr);
    }

    public StatKeys<K> replaceCost(Cost cost) {
        if(BaseUtils.hashEquals(cost, this.cost))
            return this;
        return new StatKeys<>(cost, stat, distinct, false);
    }

    public StatKeys<K> replaceStat(Stat stat) {
        if(BaseUtils.hashEquals(stat, this.stat))
            return this;
        return create(cost, stat, distinct.min(stat)); // может уменьшить distinct'ы
    }

    public static <K> StatKeys<K> create(Cost cost, Stat stat, DistinctKeys<K> distinct) {
        return new StatKeys<>(cost, distinct.getMax().min(stat), distinct);
    }
    public StatKeys<K> min(StatKeys<K> statKeys) {
        return create(cost.min(statKeys.cost), stat.min(statKeys.stat), distinct.min(statKeys.distinct));
    }

    @Override
    public String toString() {
        return "c : "+cost + ", s : " + stat + ", d : " + distinct;
    }
}

