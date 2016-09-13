package lsfusion.server.data.query.stat;

import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSSVSMap;
import lsfusion.base.col.lru.LRUWVSMap;
import lsfusion.server.Settings;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.Table;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.query.*;
import lsfusion.server.data.query.*;
import lsfusion.server.data.query.innerjoins.AbstractUpWhere;
import lsfusion.server.data.query.innerjoins.KeyEqual;
import lsfusion.server.data.query.innerjoins.UpWhere;
import lsfusion.server.data.query.innerjoins.UpWheres;
import lsfusion.server.data.translator.JoinExprTranslator;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.where.DNFWheres;
import lsfusion.server.data.where.Where;
import lsfusion.utils.GreedyTreeBuilding;
import lsfusion.utils.SpanningTreeWithBlackjack;
import lsfusion.utils.prim.Prim;
import lsfusion.utils.prim.UndirectedGraph;

import java.util.*;

public class WhereJoins extends ExtraMultiIntersectSetWhere<WhereJoin, WhereJoins> implements DNFWheres.Interface<WhereJoins>, OuterContext<WhereJoins> {

    private WhereJoins() {
    }

    protected WhereJoins FALSETHIS() {
        return WhereJoins.EMPTY;
    }
    
    public int getAllChildrenCount() {
        MSet<WhereJoin> allJoins = SetFact.mSet();
        for(WhereJoin where : wheres) {
            allJoins.addAll(getAllChildren(where));
        }
        return allJoins.size();
    }

    public int getOrderTopCount() {
        int orderTopCount = 0;
        for(WhereJoin where : wheres)
            if((where instanceof ExprIndexedJoin) && ((ExprIndexedJoin)where).isOrderTop())
                orderTopCount++;
        return orderTopCount;
    }

    private final static LRUWVSMap<WhereJoin, ImSet<WhereJoin>> cacheAllChildren = new LRUWVSMap<>(LRUUtil.L1);

    public static ImSet<WhereJoin> getAllChildren(WhereJoin where) {
        ImSet<WhereJoin> result = cacheAllChildren.get(where);
        if(result == null) {
            result = BaseUtils.getAllChildren(where, getJoins);
            cacheAllChildren.put(where, result);
        }
        return result;
    }

    private final static LRUWSSVSMap<WhereJoins, ImSet, KeyStat, StatKeys> cachePackStatKeys = new LRUWSSVSMap<>(LRUUtil.L1);
    // можно было бы локальный кэш как и сверху сделать, но также как и для children будет сильно мусорить в алгоритме
    public <K extends BaseExpr> StatKeys<K> getPackStatKeys(ImSet<K> groups, KeyStat keyStat, StatType type) {
        StatKeys result = cachePackStatKeys.get(this, groups, keyStat);
        // assert что type всегда одинаковый, хотя и keyStat по идее должен быть ???
        assert type == StatType.PACK;
        if(result==null) {
            result = getStatKeys(groups, keyStat, type);
            cachePackStatKeys.put(this, groups, keyStat, result);
        }
        return result;
    }


    private static BaseUtils.ExChildrenInterface<WhereJoin> getJoins = new BaseUtils.ExChildrenInterface<WhereJoin>() {
        public Iterable<WhereJoin> getChildrenIt(WhereJoin element) {
            return BaseUtils.immutableCast(element.getJoinFollows(new Result<UpWheres<InnerJoin>>(), null).it());
        }

        public ImSet<WhereJoin> getAllChildren(WhereJoin element) {
            return WhereJoins.getAllChildren(element);
        }
    };
    protected WhereJoin[] intersect(WhereJoin where1, WhereJoin where2) {
        ImSet<WhereJoin> common = BaseUtils.commonChildren(where1, where2, getJoins);
        return common.toArray(new WhereJoin[common.size()]);
    }

    protected WhereJoin add(WhereJoin addWhere, WhereJoin[] wheres, int numWheres, WhereJoin[] proceeded, int numProceeded) {
        return null;
    }

    public static WhereJoins EMPTY = new WhereJoins(); 

    public WhereJoins(WhereJoin[] wheres) {
        super(wheres);
    }

    public WhereJoins(ImSet<WhereJoin> wheres) {
        super(wheres.toOrderSet().toArray(new WhereJoin[wheres.size()]));
    }

    public WhereJoins(WhereJoin where) {
        super(where);
    }

    protected WhereJoins createThis(WhereJoin[] wheres) {
        return new WhereJoins(wheres);
    }

    protected WhereJoin[] newArray(int size) {
        return new WhereJoin[size];
    }

    protected boolean containsAll(WhereJoin who, WhereJoin what) {
        return BaseUtils.hashEquals(who,what) || (what instanceof InnerJoin && ((InnerJoin)what).getInnerExpr(who)!=null);
    }

    // аналог верхнего для BaseJoin
    protected static boolean containsJoinAll(BaseJoin who, WhereJoin what) {
        return BaseUtils.hashEquals(who,what) || (what instanceof InnerJoin && QueryJoin.getInnerExpr(((InnerJoin)what), who)!=null);
    }

    public WhereJoins and(WhereJoins set) {
        return add(set);
    }

    public WhereJoins or(WhereJoins set) {
        return intersect(set);
    }

    public boolean means(WhereJoins set) {
        return equals(and(set));
    }

    private InnerJoins innerJoins;
    @ManualLazy
    public InnerJoins getInnerJoins() {
        if(innerJoins == null) {
            InnerJoins calcInnerJoins = InnerJoins.EMPTY;
            for(WhereJoin where : wheres)
                calcInnerJoins = calcInnerJoins.and(where.getInnerJoins());
            innerJoins = calcInnerJoins;
        }
        return innerJoins;
    }

    public int hashOuter(HashContext hashContext) {
        int hash = 0;
        for(WhereJoin where : wheres)
            hash += where.hashOuter(hashContext);
        return hash;
    }

    public WhereJoins translateOuter(MapTranslate translator) {
        WhereJoin[] transJoins = new WhereJoin[wheres.length];
        for(int i=0;i<wheres.length;i++)
            transJoins[i] = wheres[i].translateOuter(translator);
        return new WhereJoins(transJoins);
    }

    public ImSet<OuterContext> getOuterDepends() {
        return SetFact.<OuterContext>toExclSet(wheres);
    }

    private static class Edge<K> implements GreedyTreeBuilding.Edge<BaseJoin> {
        public final BaseJoin<K> join;
        public final K key;
        public final BaseExpr expr;

        @Override
        public BaseJoin getFrom() {
            return expr.getBaseJoin();
        }

        @Override
        public BaseJoin getTo() {
            return join;
        }

        public Stat getKeyStat(MAddMap<BaseJoin, Stat> joinStats, MAddMap<Edge, Stat> keyStats) {
            return keyStats.get(this).min(joinStats.get(join));
        }
        public Stat getPropStat(MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, Stat> propStats) {
            return WhereJoins.getPropStat(expr, joinStats, propStats);
        }

        private Edge(BaseJoin<K> join, K key, BaseExpr expr) {
            this.join = join;
            this.key = key;
            this.expr = expr;
        }

        // так как добавляется для join'а и ключа, каждый из которых уникален
//        public boolean equals(Object o) {
//            return (this == o || (o instanceof Edge && join.equals(((Edge<?>) o).join) && key.equals(((Edge<?>) o).key) && expr.equals(((Edge<?>) o).expr)));
//        }

//        public int hashCode() {
//            return 31 * (31 * join.hashCode() + key.hashCode()) + expr.hashCode();
//        }

        @Override
        public String toString() {
            return expr + " -> " + join + "," + key;
        }
    }

    private static Stat getPropStat(BaseExpr valueExpr, MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, Stat> propStats) {
        return propStats.get(valueExpr).min(joinStats.get(valueExpr.getBaseJoin()));
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, KeyStat keyStat, StatType type) {
        return getStatKeys(groups, null, keyStat, type);
    }

    private final static SimpleAddValue<Object, Stat> minStat = new SymmAddValue<Object, Stat>() {
        public Stat addValue(Object key, Stat prevValue, Stat newValue) {
            return prevValue.min(newValue);
        }
    };
    private static <T> SymmAddValue<T, Stat> minStat() {
        return (SymmAddValue<T, Stat>) minStat;
    }

    public static <K> ImMap<K, BaseExpr> getJoinsForStat(BaseJoin<K> join) { // нужно чтобы не терялись ключи у Union в статистике, всегда добавлять их нельзя так как начнет следствия notNull рушить (для NotNullParams)
        if(join instanceof UnionJoin)
            return (ImMap<K, BaseExpr>) ((UnionJoin) join).getJoins(true);
        return join.getJoins();
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, Result<Stat> rows, final KeyStat keyStat, StatType type) {
        return getStatKeys(groups, rows, keyStat, type, null);
    }

    private static boolean useCost = true;

    private static long maxDiff = 0;

    // assert что rows >= result
    // можно rows в StatKeys было закинуть как и Cost, но используется только в одном месте и могут быть проблемы с кэшированием
    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, Result<Stat> rows, final KeyStat keyStat, StatType type, Result<ImSet<BaseExpr>> usedNotNullJoins) {

//        StatKeys<K> costStatKeys = getCostStatKeys(groups, rows, keyStat, type, usedNotNullJoins);
//        StatKeys<K> oldStatKeys = getOldStatKeys(groups, rows, keyStat, type);
//        StatKeys<K> exactOldStatKeys = getExactOldStatKeys(groups, rows, keyStat, type);
//
//        if(!BaseUtils.hashEquals(costStatKeys, oldStatKeys)) {
//            exactOldStatKeys = exactOldStatKeys;
//            if(!(BaseUtils.hashEquals(costStatKeys.getRows(), exactOldStatKeys.getRows()) && BaseUtils.hashEquals(costStatKeys.getDistinct(), exactOldStatKeys.getDistinct())))
//                exactOldStatKeys = exactOldStatKeys;
//        }
//
//        if(1==1) return costStatKeys;

        if(useCost || usedNotNullJoins != null)
            return getCostStatKeys(groups, rows, keyStat, type, usedNotNullJoins);
        else
            return getExactOldStatKeys(groups, rows, keyStat, type);
    }

    private <K extends BaseExpr> StatKeys<K> getOldStatKeys(ImSet<K> groups, Result<Stat> rows, KeyStat keyStat, StatType type) {
        // groups учавствует только в дополнительном фильтре
        final MAddMap<BaseJoin, Stat> joinStats = MapFact.mAddOverrideMap();
        final MAddMap<BaseExpr, Stat> exprStats = MapFact.mAddOverrideMap();
        final MAddMap<BaseJoin, Cost> indexedStats = MapFact.<BaseJoin, Cost>mAddOverrideMap();

        Result<ImMap<Edge, Stat>> edgeStats = new Result<>(); // assert edge.expr == key
        buildBalancedGraph(groups, joinStats, exprStats, edgeStats, indexedStats, type, keyStat);

        final Stat finalStat = getStat(joinStats, edgeStats.result);
        if(rows!=null)
            rows.set(finalStat);

        DistinctKeys<K> distinct = new DistinctKeys<>(groups.mapValues(new GetValue<Stat, K>() {
            public Stat getMapValue(K value) { // для groups, берем min(из статистики значения, статистики его join'а)
                return getPropStat(value, joinStats, exprStats).min(finalStat);
            }
        }));
        //, Cost cost
        return StatKeys.create(getIndexedStat(indexedStats, finalStat), finalStat, distinct);
    }

    private static class JoinCostStat<Z> extends CostStat {
        private final BaseJoin<Z> join;
        private final StatKeys<Z> statKeys;

        public JoinCostStat(BaseJoin<Z> join, StatKeys<Z> statKeys) {
            this(join, statKeys, SetFact.<BaseExpr>EMPTY());
        }
        public JoinCostStat(BaseJoin<Z> join, StatKeys<Z> statKeys, ImSet<BaseExpr> usedNotNulls) {
            super(usedNotNulls);
            this.join = join;
            this.statKeys = statKeys;
        }

        @Override
        public Cost getCost() {
            return statKeys.getCost();
        }

        @Override
        public Stat getStat() {
            return statKeys.getRows();
        }

        @Override
        public Cost getMaxCost() {
            return getCost();
        }

        @Override
        public Stat getMinStat() {
            return getStat();
        }

        @Override
        public Stat getMaxStat() {
            return getStat();
        }

        @Override
        public int getJoinsCount() {
            return 1;
        }

        @Override
        public ImSet<BaseJoin> getJoins() {
            return SetFact.singleton((BaseJoin) join);
        }

        @Override
        public ImMap<BaseJoin, Stat> getJoinStats() {
            return MapFact.singleton((BaseJoin)join, statKeys.getRows());
        }

        @Override
        public ImMap<BaseJoin, DistinctKeys> getKeyStats() {
            return MapFact.singleton((BaseJoin)join, (DistinctKeys) statKeys.getDistinct());
        }

        @Override
        public ImMap<BaseExpr, Stat> getPropStats() {
            return MapFact.EMPTY();
        }

        @Override
        public PropStat getPropStat(BaseExpr expr, MAddMap<BaseExpr, PropStat> exprStats) {
            assert BaseUtils.hashEquals(expr.getBaseJoin(), join);
            PropStat exprStat = exprStats.get(expr);
//            assert exprStat.distinct.lessEquals(statKeys.getRows()) && (exprStat.notNull == null || exprStat.notNull.lessEquals(statKeys.getRows())); // при start'е иногда по умолчанию значения похоже заполняются
            return exprStat;
        }

        @Override
        public <K> Stat getKeyStat(BaseJoin<K> baseJoin, K key) {
            assert BaseUtils.hashEquals(baseJoin, join);
            return statKeys.getDistinct((Z)key);
        }

        @Override
        public ImSet getPushKeys() {
            return null;
        }

        @Override
        protected StatKeys getPushStatKeys() {
            return statKeys;
        }

        @Override
        public String toString(String prefix) {
            return prefix + join + " " + statKeys + " " + join.getJoins();
        }
    }

    private static class MergeCostStat extends CostStat {

        // основные параметры
        private final Cost cost;
        private final Stat stat;

        // доппараметры, в основном для детерменированности
        private final Cost maxCost;
        private final Stat leftStat;
        private final Stat rightStat;
        private final int joinsCount;

        // путь
        private final ImMap<BaseJoin, Stat> joinStats;  // минимум по статистике с момента появления этого join'а в дереве;
        private final ImMap<BaseJoin, DistinctKeys> keyStats; // поддерживаем только потому что getPushedStatKeys может их "уточнять"
        private final ImMap<BaseExpr, Stat> propStats; // поддерживаем только потому что getPushedStatKeys может их "уточнять"

        // проталкивание
        private final ImSet pushKeys;
        private final StatKeys pushStatKeys; // важно получить хороший именно pushStatKeys (то есть проталкивание), а не финальную статистику

        // debug info, temporary
        private final CostStat left;
        private final CostStat right;
        private final Stat[] aEdgeStats;
        private final Stat[] bEdgeStats;
        private final List<? extends Edge> edges;

        public MergeCostStat(Cost cost, Stat stat,
                             Cost maxCost, Stat leftStat, Stat rightStat, int joinsCount,
                             CostStat left, CostStat right, Stat[] aEdgeStats, Stat[] bEdgeStats, List<? extends Edge> edges) {
            this(cost, stat,
                    maxCost, leftStat, rightStat, joinsCount,
                    null, null, null, null, null, null,
                    left, right, aEdgeStats, bEdgeStats, edges);
        }

        public MergeCostStat(Cost cost, Stat stat,
                             Cost maxCost, Stat leftStat, Stat rightStat, int joinsCount,
                             ImMap<BaseJoin, Stat> joinStats, ImMap<BaseJoin, DistinctKeys> keyStats, ImMap<BaseExpr, Stat> propStats, ImSet pushKeys, StatKeys pushStatKeys, ImSet<BaseExpr> usedNotNulls,
                             CostStat left, CostStat right, Stat[] aEdgeStats, Stat[] bEdgeStats, List<? extends Edge> edges) {
            super(usedNotNulls);
            this.cost = cost;
            this.stat = stat;

            this.maxCost = maxCost;
            this.leftStat = leftStat;
            this.rightStat = rightStat;
            this.joinsCount = joinsCount;

            this.joinStats = joinStats;
            this.keyStats = keyStats;
            this.propStats = propStats; // assert что все expr.getBaseJoin() из joinStats

            this.pushKeys = pushKeys;
            this.pushStatKeys = pushStatKeys;

            this.left = left;
            this.right = right;
            this.aEdgeStats = aEdgeStats;
            this.bEdgeStats = bEdgeStats;
            this.edges = edges;
        }

        @Override
        public Cost getCost() {
            return cost;
        }

        @Override
        public Stat getStat() {
            return stat;
        }

        @Override
        public Cost getMaxCost() {
            return maxCost;
        }

        @Override
        public Stat getMinStat() {
            return leftStat.min(rightStat);
        }

        @Override
        public Stat getMaxStat() {
            return leftStat.max(rightStat);
        }

        @Override
        public int getJoinsCount() {
            return joinsCount;
        }

        @Override
        public ImSet<BaseJoin> getJoins() {
            return joinStats.keys();
        }

        @Override
        public ImMap<BaseJoin, Stat> getJoinStats() {
            return joinStats;
        }

        @Override
        public ImMap<BaseJoin, DistinctKeys> getKeyStats() {
            return keyStats;
        }

        @Override
        public ImMap<BaseExpr, Stat> getPropStats() {
            return propStats;
        }

        @Override
        public PropStat getPropStat(BaseExpr expr, MAddMap<BaseExpr, PropStat> exprStats) {

            Stat minJoinStat = joinStats.get(expr.getBaseJoin());

            // тут еще надо к notNull добавить (stat - minStat)

            Stat distinct;
            Stat nullFrac = Stat.ONE;
            PropStat exprStat;
            Stat propStat = propStats.get(expr);
            if(propStat != null) {
                assert propStat.less(exprStats.get(expr).distinct);
                distinct = propStat; // assert что notNull, так как join уже редуцировался по notNull
            } else {
                exprStat = exprStats.get(expr);
                distinct = exprStat.distinct;
                if(exprStat.notNull != null && exprStat.notNull.less(minJoinStat))
                    nullFrac = minJoinStat.div(exprStat.notNull); // по хорошему надо еще учитывать maxJoinStat, но тут и так много допущений, поэтому эта эвристика должна более менее эффективно работать
            }
            return new PropStat(distinct.min(minJoinStat), stat.div(nullFrac));
        }

        @Override
        public <K> Stat getKeyStat(BaseJoin<K> baseJoin, K key) {
            DistinctKeys<K> keyStat = keyStats.get(baseJoin);
            return joinStats.get(baseJoin).min(keyStat.get(key));
        }

        @Override
        public ImSet getPushKeys() {
            return pushKeys;
        }

        @Override
        protected StatKeys getPushStatKeys() {
            return pushStatKeys;
        }

        public String toString(String prefix) {
            return prefix + "m" + getCost() + " " + getStat() + " LEFT : " + Arrays.toString(aEdgeStats) + " RIGHT : " + Arrays.toString(bEdgeStats) + "\n" + left.toString(prefix + '\t') + '\n' + right.toString(prefix + '\t');
        }
    }

    private abstract static class CostStat implements Comparable<CostStat> {

        public CostStat(ImSet<BaseExpr> usedNotNulls) {
            this.usedNotNulls = usedNotNulls;
        }

        public abstract Cost getCost();
        public abstract Stat getStat();

        public abstract Cost getMaxCost();
        public abstract Stat getMinStat();
        public abstract Stat getMaxStat();
        public abstract int getJoinsCount();

        public abstract ImSet<BaseJoin> getJoins();
        public abstract ImMap<BaseJoin, Stat> getJoinStats();
        public abstract ImMap<BaseJoin, DistinctKeys> getKeyStats();
        public abstract ImMap<BaseExpr, Stat> getPropStats();

        public abstract PropStat getPropStat(BaseExpr expr, MAddMap<BaseExpr, PropStat> exprStats);
        public abstract <K> Stat getKeyStat(BaseJoin<K> join, K key);

        protected final ImSet<BaseExpr> usedNotNulls;
        public abstract ImSet getPushKeys();

        private <K> PropStat getPropStat(Edge<K> edge, MAddMap<BaseExpr, PropStat> exprStats) {
            return getPropStat(edge.expr, exprStats);
        }

        private <K> Stat getKeyStat(Edge<K> edge) {
            return getKeyStat(edge.join, edge.key);
        }

        private <K extends BaseExpr> ImMap<K, Stat> getDistinct(ImSet<K> exprs, final MAddMap<BaseExpr, PropStat> exprStats) {
            return new DistinctKeys<>(exprs.mapValues(new GetValue<Stat, K>() {
                public Stat getMapValue(K value) {
                    return getPropStat(value, exprStats).distinct;
                }
            }));
        }

        private static int pushCompareTo(StatKeys a, StatKeys b) {
            int compare = Boolean.compare(a == null, b == null);
            if(compare != 0)
                return compare;
            if(a == null || b == null)
                return 0;
            compare = Integer.compare(a.getCost().rows.getWeight(), b.getCost().rows.getWeight());
            if(compare != 0)
                return compare;
            compare = Integer.compare(a.getRows().getWeight(), b.getRows().getWeight());
            if(compare != 0)
                return compare;
            return 0;
        }

        protected abstract StatKeys getPushStatKeys();
        public int pushCompareTo(CostStat o) {
            MergeCostStat mStat = (MergeCostStat) o;
            return pushCompareTo(getPushStatKeys(), mStat.pushStatKeys);
        }

        @Override
        public int compareTo(CostStat o) {
            int compare = Integer.compare(getCost().rows.getWeight(), o.getCost().rows.getWeight());
            if(compare != 0)
                return compare;
            compare = Integer.compare(getStat().getWeight(), o.getStat().getWeight());
            if(compare != 0)
                return compare;
            compare = Integer.compare(getMaxCost().rows.getWeight(), o.getMaxCost().rows.getWeight());
            if(compare != 0) // у кого max cost больше лучше
                return -compare;
            compare = Integer.compare(getMaxStat().getWeight(), o.getMaxStat().getWeight());
            if(compare != 0) // у кого max больше лучше
                return -compare;
            compare = Integer.compare(getMinStat().getWeight(), o.getMinStat().getWeight());
            if(compare != 0) // у кого min больше лучше
                return -compare;
            return Integer.compare(getJoinsCount(), o.getJoinsCount()); // берем меньшее дерево
        }

        public abstract String toString(String prefix);

        @Override
        public String toString() {
            return toString("");
        }
    }

    private interface CostResult<T> {
        T calculate(CostStat costStat, ImSet<Edge> edges, MAddMap<BaseExpr, PropStat> exprStats);
    }

    public <K extends BaseExpr, T> T calculateCost(ImSet<K> groups, QueryJoin join, boolean needNotNulls, final KeyStat keyStat, final StatType type, CostResult<T> result) {
        final MAddMap<BaseJoin, Stat> joinStats = MapFact.mAddOverrideMap();
        final MAddMap<BaseJoin, DistinctKeys> keyDistinctStats = MapFact.mAddOverrideMap();
        final MAddMap<BaseExpr, PropStat> exprStats = MapFact.mAddOverrideMap();
        final MAddMap<BaseJoin, Cost> indexedStats = MapFact.<BaseJoin, Cost>mAddOverrideMap();
        Result<ImSet<Edge>> edges = new Result<>();

        buildGraphWithStats(groups, edges, joinStats, exprStats, null, keyDistinctStats, indexedStats, type, keyStat, join);

        CostStat costStat = getCost(join, needNotNulls, joinStats, indexedStats, exprStats, keyDistinctStats, edges.result, keyStat, type);

        return result.calculate(costStat, edges.result, exprStats);
    }

    public <K extends BaseExpr, Z> StatKeys<K> getCostStatKeys(final ImSet<K> groups, final Result<Stat> rows, final KeyStat keyStat, final StatType type, final Result<ImSet<BaseExpr>> usedNotNullJoins) {
        // нужно отдельно STAT считать, так как при например 0 - 3, 0 - 100 получит 3 - 100 -> 3, а не 0 - 3 -> 3 и соотвественно статистику 3, а не 0
        //  но пока не принципиально будем брать stat из "плана"

        if(isFalse() && groups.isEmpty()) {
            if(rows != null)
                rows.set(Stat.ONE);
            if(usedNotNullJoins != null)
                usedNotNullJoins.set(SetFact.<BaseExpr>EMPTY());
            return new StatKeys<K>(groups, Stat.ONE);
        }

        return calculateCost(groups, null, usedNotNullJoins != null, keyStat, type, new CostResult<StatKeys<K>>() {
            public StatKeys<K> calculate(CostStat costStat, ImSet<Edge> edges, MAddMap<BaseExpr, PropStat> exprStats) {
                Stat stat = costStat.getStat();
                Cost cost = costStat.getCost();
                if(rows != null)
                    rows.set(stat);
                if(usedNotNullJoins != null)
                    usedNotNullJoins.set(costStat.usedNotNulls);
                return StatKeys.create(cost, stat, new DistinctKeys<>(costStat.getDistinct(groups, exprStats)));
            }
        });
    }

    private <K> StatKeys<K> create(Cost cost, Stat stat, ImMap<K, Stat> distinct) {
        return StatKeys.create(cost, stat, new DistinctKeys<>(distinct));
    }

    public <K extends BaseExpr, Z extends Expr> Where getCostPushWhere(final QueryJoin<Z, ?, ?, ?> queryJoin, final UpWheres<WhereJoin> upWheres, final KeyStat keyStat, final StatType type) {
        ImSet<BaseExpr> groups = queryJoin.getJoins().values().toSet();


        return calculateCost(groups, queryJoin, false, keyStat, type, new CostResult<Where>() {
            public Where calculate(CostStat costStat, ImSet<Edge> edges, MAddMap<BaseExpr, PropStat> exprStats) {
                return getCostPushWhere(costStat, edges, queryJoin, upWheres);
            }
        });
    }

    private boolean recProceedChildrenCostWhere(BaseJoin join, MAddExclMap<BaseJoin, Boolean> proceeded, MMap<BaseJoin, MiddleTreeKeep> mMiddleTreeKeeps, MSet<BaseExpr> mAllKeeps, MSet<BaseExpr> mTranslate, boolean keepThis, ImSet<BaseJoin> keepJoins, ImMap<BaseJoin, ImSet<Edge>> inEdges) {
        Boolean cachedAllKeep = proceeded.get(join);
        if(cachedAllKeep != null)
            return cachedAllKeep;

        ImSet<Edge> inJoin = inEdges.get(join);
        if(inJoin == null)
            inJoin = SetFact.EMPTY();

        MSet<BaseExpr> mInAllKeeps = SetFact.mSetMax(inJoin.size()); // все "полные" children

        boolean allKeep = keepThis;
        for (Edge edge : inJoin) {
            BaseJoin fromJoin = edge.getFrom();
            boolean inAllKeep = recProceedCostWhere(fromJoin, proceeded, mMiddleTreeKeeps, mAllKeeps, mTranslate, edge, keepThis, keepJoins.contains(fromJoin), keepJoins, inEdges);
            allKeep = inAllKeep && allKeep;
            if(inAllKeep)
                mInAllKeeps.add(edge.expr);
        }
        if(keepThis && !allKeep) // если этот элемент не "полный", значит понадобятся все child'ы для трансляции, соотвественно пометим "полные" из них
            mAllKeeps.addAll(mInAllKeeps.immutable());

        proceeded.exclAdd(join, allKeep);
        return allKeep;
    }

    private boolean recProceedCostWhere(BaseJoin join, MAddExclMap<BaseJoin, Boolean> proceeded, MMap<BaseJoin, MiddleTreeKeep> mMiddleTreeKeeps, MSet<BaseExpr> mAllKeeps, MSet<BaseExpr> mTranslate, Edge upEdge, boolean upKeep, boolean keepThis, ImSet<BaseJoin> keepJoins, ImMap<BaseJoin, ImSet<Edge>> inEdges) {
        assert keepThis == keepJoins.contains(join);
        if(!keepThis && upKeep && (join instanceof ParamExpr || join instanceof ValueJoin)) // ParamExpr и ValueJoin принудительно делаем keep
            keepThis = true;

        boolean allKeep = recProceedChildrenCostWhere(join, proceeded, mMiddleTreeKeeps, mAllKeeps, mTranslate, keepThis, keepJoins, inEdges);

        if (keepThis) // есть верхний keep join, соответственно это его проблема добавить Where (этот сам "подцепится" после этого)
            mMiddleTreeKeeps.add(join, upKeep ? IntermediateKeep.instance : new MiddleTopKeep(upEdge.expr)); // есть ребро "наверх", используем выражение из него
        else
            if (upKeep) // если был keep, а этот не нужен - добавляем трансляцию
                mTranslate.add(upEdge.expr);

        return allKeep;
    }

    private interface Keep {
    }

    private static abstract class AKeep implements Keep {

    }

    private final static AddValue<BaseJoin, MiddleTreeKeep> addKeepValue = new SymmAddValue<BaseJoin, MiddleTreeKeep>() {
        public MiddleTreeKeep addValue(BaseJoin key, MiddleTreeKeep prevValue, MiddleTreeKeep newValue) {
            if(prevValue == IntermediateKeep.instance || newValue == IntermediateKeep.instance) // intermediate приоритетнее middleTopKeep
                return IntermediateKeep.instance;
            return prevValue; // тут не важно, оставлять старое или брать новое
        }
    };

    private interface MiddleTreeKeep extends Keep  {
    }

    private static class IntermediateKeep extends AKeep implements MiddleTreeKeep {

        private static final IntermediateKeep instance = new IntermediateKeep();
    }

    private static abstract class TopKeep extends AKeep implements Keep {
        public abstract Where getWhere(BaseJoin join, UpWheres<WhereJoin> upWheres);
    }

    private static class MiddleTopKeep extends TopKeep implements MiddleTreeKeep {
        private final BaseExpr expr;

        public MiddleTopKeep(BaseExpr expr) {
            this.expr = expr;
        }

        public Where getWhere(BaseJoin join, UpWheres<WhereJoin> upWheres) {
            return expr.getWhere();
        }
    }

    private static class TopTreeKeep extends TopKeep {
        private static final TopTreeKeep instance = new TopTreeKeep();

        @Override
        public Where getWhere(BaseJoin join, UpWheres<WhereJoin> upWheres) {
            return getUpWhere((WhereJoin) join, upWheres.get((WhereJoin) join));
        }
    }

    private static boolean keep(BaseJoin from, ImSet<BaseJoin> keepJoins) {
        if(from instanceof ParamExpr || from instanceof ValueJoin) // докидываем ParamExpr, потом надо будет оптимизировать если будет критично, чтобы не пересоздавать коллекци с merge
            return true;

        return false;
    }

    private <Z extends Expr> Where getCostPushWhere(CostStat cost, ImSet<Edge> edges, QueryJoin<Z, ?, ?, ?> queryJoin, UpWheres<WhereJoin> upWheres) {
        ImSet<Z> pushedKeys = (ImSet<Z>) cost.getPushKeys();
        if(pushedKeys == null) { // значит ничего не протолкнулось
            // пока падает из-за неправильного computeVertex видимо
//            assert BaseUtils.hashEquals(SetFact.singleton(queryJoin), cost.getJoins());
            return null;
        }
        ImSet<BaseJoin> keepJoins = cost.getJoins().removeIncl(queryJoin);
        ImMap<Z, BaseExpr> pushMap = queryJoin.getJoins().filterIncl(pushedKeys);

        final ImMap<BaseJoin, ImSet<Edge>> inEdges = edges.group(new BaseUtils.Group<BaseJoin, Edge>() {
            public BaseJoin group(Edge value) {
                return value.getTo();
            }});

        MSet<BaseExpr> mFullExprs = SetFact.mSet();
        MSet<BaseExpr> mTranslate = SetFact.mSet();

        Result<UpWheres<WhereJoin>> upAdjWheres = new Result<>(upWheres);
        List<WhereJoin> adjWheres = getAdjIntervalWheres(upAdjWheres);
        upWheres = upAdjWheres.result;

        MExclSet<WhereJoin> mTopKeys = SetFact.mExclSetMax(adjWheres.size());
        MMap<BaseJoin, MiddleTreeKeep> mMiddleTreeKeeps = MapFact.mMap(addKeepValue);

        MAddExclMap<BaseJoin, Boolean> proceeded = MapFact.mAddExclMap();
        for(WhereJoin where : adjWheres) { // бежим по upWhere
            boolean keepThis = keepJoins.contains(where);

            recProceedChildrenCostWhere(where, proceeded, mMiddleTreeKeeps, mFullExprs, mTranslate, keepThis, keepJoins, inEdges);

            if(keepThis)
                mTopKeys.exclAdd(where);
        }
        // !!! СНАЧАЛА TRANSLATE'М , а потом AND'м, так как Expr'ы могут измениться, тоже самое касается UpWhere - translate'им потом делаем getWhere ??? хотя можно это позже сделать ???
        // UPWHERE, берем все вершины keep у которых нет исходящих в keep (не "промежуточные"), если есть в upWheres берем оттуда, иначе берем первый попавшийся edge у вершины из которой нет выходов (проблема правда в том что InnerFollows не попадут и можно было бы взять класс вместо значения, но это не критично)

        ImMap<BaseJoin, MiddleTopKeep> middleTopKeeps = BaseUtils.immutableCast(mMiddleTreeKeeps.immutable().filterFnValues(new SFunctionSet<MiddleTreeKeep>() {
            public boolean contains(MiddleTreeKeep element) {
                return element instanceof MiddleTopKeep;
            }
        }));
        JoinExprTranslator translator = new JoinExprTranslator(KeyExpr.getMapKeys(mTranslate.immutable()), mFullExprs.immutable());
        Where upPushWhere = Where.TRUE;
        ImMap<BaseJoin, TopKeep> keeps = MapFact.addExcl(mTopKeys.immutable().toMap(TopTreeKeep.instance), middleTopKeeps);
        for(int i=0,size=keeps.size();i<size;i++) {
            BaseJoin join = keeps.getKey(i);
            TopKeep keep = keeps.getValue(i);

            Where upJoinWhere = keep.getWhere(join, upWheres);

            boolean allKeep = proceeded.get(join);
            if(!allKeep)
                upJoinWhere = upJoinWhere.translateExpr(translator);
            else
                assert BaseUtils.hashEquals(upJoinWhere, upJoinWhere.translateExpr(translator));

            upPushWhere = upPushWhere.and(upJoinWhere);
        }

        Result<Where> pushExtraWhere = new Result<>(); // для partition
        ImMap<Expr, ? extends Expr> translatedPush = queryJoin.getPushGroup(translator.translate(pushMap), true, pushExtraWhere);
        if(pushExtraWhere.result != null)
            upPushWhere = upPushWhere.and(pushExtraWhere.result.translateExpr(translator));
        return GroupExpr.create(translatedPush, upPushWhere, translatedPush.keys().toMap()).getWhere();
    }

    private <K extends BaseExpr, Z> CostStat getCost(final QueryJoin pushJoin, final boolean needNotNulls, MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseJoin, Cost> indexedStats, final MAddMap<BaseExpr, PropStat> exprStats, MAddMap<BaseJoin, DistinctKeys> keyDistinctStats, ImSet<Edge> edges, final KeyStat keyStat, final StatType type) {
        // отдельно считаем cost
        final GreedyTreeBuilding.CalculateCost<BaseJoin, CostStat, Edge<K>> costCalc = getCostFunc(pushJoin, exprStats, needNotNulls, keyStat, type);

        CostStat result;
        CostStat pushCost = null;
        assert joinStats.size() > 0;
        final GreedyTreeBuilding<BaseJoin, CostStat, Edge<K>> treeBuilding = new GreedyTreeBuilding<>();
        for (int i = 0, size = joinStats.size(); i < size; i++) {
            BaseJoin join = joinStats.getKey(i);
            JoinCostStat joinCost = new JoinCostStat(join, new StatKeys(indexedStats.get(join), joinStats.getValue(i), keyDistinctStats.get(join)));

            if (pushJoin != null && BaseUtils.hashEquals(join, pushJoin))
                pushCost = joinCost;

            treeBuilding.addVertex(join, joinCost);
        }

        for (Edge edge : edges)
            treeBuilding.addEdge(edge);

        GreedyTreeBuilding.TreeNode<BaseJoin, CostStat> compute;
        if (pushJoin != null) {
            assert joinStats.containsKey(pushJoin);
            if(joinStats.size() == 1)
                return pushCost;

            compute = treeBuilding.computeWithVertex(pushJoin, costCalc, new GreedyTreeBuilding.TreeCutComparator<CostStat>() {
                public int compare(CostStat a, CostStat b) {
                    return a.pushCompareTo(b);
                }});
        } else
            compute = treeBuilding.compute(costCalc);
        result = compute.node.getCost();

        if(pushJoin != null && pushCost.pushCompareTo(result) <= 0) // так как текущий computeWithVertex всегда берет хоть одно ребро
            return pushCost;
        else
            return result;
    }

    private static <K extends BaseExpr, Z> GreedyTreeBuilding.CalculateCost<BaseJoin, CostStat, Edge<K>> getCostFunc(final QueryJoin pushJoin, final MAddMap<BaseExpr, PropStat> exprStats, final boolean needNotNulls, final KeyStat keyStat, final StatType type) {
        return new GreedyTreeBuilding.CalculateCost<BaseJoin, CostStat, Edge<K>>() {

                @Override
                public CostStat calculateLowerBound(GreedyTreeBuilding.Node<BaseJoin, CostStat> a, GreedyTreeBuilding.Node<BaseJoin, CostStat> b, Iterable<Edge<K>> edges) {

//                    if(!useLowerBound) return new MergeCostStat(Cost.MIN, Stat.MIN, Cost.ALOT, Stat.MIN, Stat.MIN, -1, null, null, null, null, null);

                    CostStat aCostStat = a.getCost();
                    CostStat bCostStat = b.getCost();
                    if (aCostStat.compareTo(bCostStat) > 0) { // будем считать что у a cost меньше то есть он "левый"
                        GreedyTreeBuilding.Node<BaseJoin, CostStat> t = a;
                        a = b;
                        b = t;
                        CostStat tCost = aCostStat;
                        aCostStat = bCostStat;
                        bCostStat = tCost;
                    }

                    Stat aAdjStat = aCostStat.getStat();
                    Stat bAdjStat = bCostStat.getStat();

                    List<Edge<K>> edgesList = null;
                    Stat[] aEdgeStats = null;
                    Stat[] bEdgeStats = null;

                    Stat newStat;
                    if(!edges.iterator().hasNext()) { // оптимизация, как самый самый самый частый случай
                        newStat = aAdjStat.mult(bAdjStat);
                    } else {
                        edgesList = BaseUtils.toList(edges);
                        int size = edgesList.size();

                        aEdgeStats = new Stat[size];
                        bEdgeStats = new Stat[size];

                        for (int i = 0; i < size; i++) {
                            Edge<K> edge = edgesList.get(i);
                            boolean aIsKey = aCostStat.getJoins().contains(edge.getTo()); // A - ключ
                            if (aIsKey) {
                                PropStat bEdgeStat = bCostStat.getPropStat(edge, exprStats);
                                if (bEdgeStat.notNull != null)
                                    bAdjStat = bAdjStat.min(bEdgeStat.notNull);
                                bEdgeStats[i] = bEdgeStat.distinct;
                                aEdgeStats[i] = aCostStat.getKeyStat(edge);
                            } else {
                                PropStat aEdgeStat = aCostStat.getPropStat(edge, exprStats);
                                if (aEdgeStat.notNull != null)
                                    aAdjStat = aAdjStat.min(aEdgeStat.notNull);
                                aEdgeStats[i] = aEdgeStat.distinct;
                                bEdgeStats[i] = bCostStat.getKeyStat(edge);
                            }
                        }

                        newStat = calcEstJoinStat(aAdjStat, bAdjStat, size, aEdgeStats, bEdgeStats, true, null, null);
                    }

                    Cost aCost = aCostStat.getCost();
                    Cost bCost = bCostStat.getCost();
                    Cost newCost = (b.getVertex() != null ? aCost.min(bCost) : aCost.or(bCost)).or(new Cost(newStat)); // если есть vertex - может протолкнуться иначе нет

                    return new MergeCostStat(newCost, newStat,
                            bCost, aAdjStat, bAdjStat, aCostStat.getJoins().size() + bCostStat.getJoins().size(),
                            aCostStat, bCostStat, aEdgeStats, bEdgeStats, edgesList);
                }

                public CostStat calculate(GreedyTreeBuilding.Node<BaseJoin, CostStat> a, GreedyTreeBuilding.Node<BaseJoin, CostStat> b, Iterable<Edge<K>> edges) {

                    // берем 2 вершины
                    CostStat aCostStat = a.getCost();
                    CostStat bCostStat = b.getCost();
                    if (aCostStat.compareTo(bCostStat) > 0) { // будем считать что у a cost меньше то есть он "левый"
                        GreedyTreeBuilding.Node<BaseJoin, CostStat> t = a;
                        a = b;
                        b = t;
                        CostStat tCost = aCostStat;
                        aCostStat = bCostStat;
                        bCostStat = tCost;
                    }

                    BaseJoin<Z> bv = b.getVertex();
                    Cost aCost = aCostStat.getCost(); // не предполагает изменение
                    Cost bBaseCost = bCostStat.getCost();
                    AddValue<BaseExpr, Stat> minStat = minStat();

                    // обрабатываем notNull'ы, важно чтобы идеологически совпадал с getPushedCost
                    List<Edge<K>> edgesList = BaseUtils.toList(edges);
                    Stat[] aEdgeStats = new Stat[edgesList.size()];
                    Stat[] bEdgeStats = new Stat[edgesList.size()];
                    Stat[] aNotNullStats = new Stat[edgesList.size()];
                    boolean[] aIsKeys = new boolean[edgesList.size()];
                    Stat aAdjStat = aCostStat.getStat();
                    Stat bAdjStat = bCostStat.getStat();

                    MAddExclMap<BaseExpr, Integer> exprs = MapFact.mAddExclMapMax(edgesList.size());
                    BaseJoin[] keyJoins = new BaseJoin[edgesList.size()];
                    Object[] keys = new Object[edgesList.size()];

                    // читаем edge'и
                    int adjEdges = 0; // с неповторяющимися expr
                    for (Edge<K> edge : edgesList) {
                        boolean wasExpr = true;
                        boolean updateKeyJoin = false;
                        Integer j = exprs.get(edge.expr);
                        if (j == null) {
                            j = adjEdges++;
                            exprs.exclAdd(edge.expr, j);

                            wasExpr = false;
                            updateKeyJoin = true;
                        }

                        boolean aIsKey;
                        if (wasExpr) {
                            aIsKey = aIsKeys[j];
                        } else {
                            aIsKey = aCostStat.getJoins().contains(edge.getTo()); // A - ключ
                            aIsKeys[j] = aIsKey;
                        }

                        if (aIsKey) {
                            if (!wasExpr) {
                                PropStat bEdgeStat = bCostStat.getPropStat(edge, exprStats);
                                if (bEdgeStat.notNull != null)
                                    bAdjStat = bAdjStat.min(bEdgeStat.notNull);
                                bEdgeStats[j] = bEdgeStat.distinct;
                            }

                            Stat aEdgeStat = aCostStat.getKeyStat(edge);
                            updateKeyJoin = updateKeyJoin || aEdgeStat.less(aEdgeStats[j]);
                            if(updateKeyJoin)
                                aEdgeStats[j] = aEdgeStat;
                        } else {
                            if (!wasExpr) {
                                PropStat aEdgeStat = aCostStat.getPropStat(edge, exprStats);
                                if (aEdgeStat.notNull != null)
                                    aAdjStat = aAdjStat.min(aEdgeStat.notNull);
                                aEdgeStats[j] = aEdgeStat.distinct;
                                aNotNullStats[j] = aEdgeStat.notNull;
                            }

                            Stat bEdgeStat = bCostStat.getKeyStat(edge);
                            updateKeyJoin = updateKeyJoin || bEdgeStat.less(bEdgeStats[j]);
                            if(updateKeyJoin)
                                bEdgeStats[j] = bEdgeStat;
                        }

                        if(updateKeyJoin) { // "переставляем" edge на элемент с меньшей статистикой
                            keys[j] = edge.key;
                            keyJoins[j] = edge.join;
                        }
                    }

                    // PUSH COST (STATS)
                    ImSet pushedKeys = null;
                    StatKeys pushedJoinStatKeys = null;
                    if(bv != null && !edgesList.isEmpty()) { // последнее - оптимизация
                        boolean useQueryStatAdjust = bv instanceof QueryJoin;

                        MExclMap<Z, Stat> mKeys = MapFact.mExclMapMax(adjEdges);
                        MExclMap<Z, Stat> mNotNullKeys = MapFact.mExclMapMax(adjEdges);
                        MAddExclMap<Z, Integer> keyIndices = MapFact.mAddExclMapMax(adjEdges);
                        MExclMap<BaseExpr, Stat> mProps = MapFact.mExclMapMax(adjEdges);
                        MExclSet<BaseExpr> mNotNullProps = needNotNulls ? SetFact.<BaseExpr>mExclSetMax(adjEdges) : null;

                        for (int k = 0, size = exprs.size(); k < size; k++) {
                            int i = exprs.getValue(k);

                            Stat aEdgeStat = aEdgeStats[i];
                            boolean aIsKey = aIsKeys[i];

                            if (aIsKey) {
                                BaseExpr expr = exprs.getKey(k);
                                mProps.exclAdd(expr, aEdgeStat);

                                if(needNotNulls) { // по хорошему надо min'ы только брать, но больше не меньше
                                    BaseJoin to = keyJoins[i];
                                    if (to instanceof ExprStatJoin && ((ExprStatJoin) to).notNull)
                                        mNotNullProps.exclAdd(expr);
                                }
                            } else {
                                Z key = (Z)keys[i];
                                mKeys.exclAdd(key, aEdgeStat);
                                if(aNotNullStats[i] != null)
                                    mNotNullKeys.exclAdd(key, aNotNullStats[i]);
                                keyIndices.exclAdd(key, i);
                            }
                        }

                        JoinCostStat<Z> bJoinCost = (JoinCostStat<Z>) bCostStat;
                        assert BaseUtils.hashEquals(bv, bJoinCost.join);

                        ImMap<Z, Stat> pushKeys = mKeys.immutable();
                        ImMap<Z, Stat> pushNotNullKeys = mNotNullKeys.immutable();
                        ImMap<BaseExpr, Stat> pushProps = mProps.immutable();
                        Stat aStat = aCostStat.getStat();

                        ImSet<BaseExpr> usedNotNulls = SetFact.EMPTY();
                        StatKeys<Z> pushedStatKeys;
                        Result<ImSet<Z>> rPushedKeys = pushJoin != null && BaseUtils.hashEquals(bv, pushJoin) ? new Result<ImSet<Z>>() : null;
                        Result<ImSet<BaseExpr>> rPushedProps = needNotNulls ? new Result<ImSet<BaseExpr>>() : null;
                        if (useQueryStatAdjust) { // для query join можно протолкнуть внутрь предикаты
                            pushedStatKeys = ((QueryJoin) bv).getPushedStatKeys(type, aCost, aStat, pushKeys, pushNotNullKeys, rPushedKeys);

                            pushedStatKeys = pushedStatKeys.min(bJoinCost.statKeys); // по идее push должен быть меньше, но из-за несовершенства статистики и отсутствия проталкивания в таблицу (pushedJoin присоединятся к маленьким join'ам и может немного увеличивать cost / stat), после "проталкивания в таблицу" можно попробовать вернуть assertion
//                                assert BaseUtils.hashEquals(pushedStatKeys.min(bJoinCost.statKeys), pushedStatKeys);

                            for(int i=0,size=keyIndices.size();i<size;i++) // обновляем bEdgeStats
                                bEdgeStats[keyIndices.getValue(i)] = pushedStatKeys.getDistinct(keyIndices.getKey(i));
                            bAdjStat = bAdjStat.min(pushedStatKeys.getRows());
                        } else {
                            Cost pushedCost = bv.getPushedCost(keyStat, type, aCost, aStat, pushKeys, pushNotNullKeys, pushProps, rPushedKeys, rPushedProps); // впоследствие можно убрать aStat добавив predicate pushDown таблицам

                            pushedCost = pushedCost.min(bJoinCost.getCost()); // по идее push должен быть меньше, но из-за несовершенства статистики и отсутствия проталкивания в таблицу (pushedJoin присоединятся к маленьким join'ам и может немного увеличивать cost / stat), после "проталкивания в таблицу" можно попробовать вернуть assertion
//                                assert bv instanceof KeyExpr || BaseUtils.hashEquals(pushedCost.min(bJoinCost.getCost()), pushedCost); // по идее push должен быть меньше

                            pushedStatKeys = bJoinCost.statKeys.replaceCost(pushedCost); // подменяем только cost
                            if (rPushedProps != null && rPushedProps.result != null) // только notNull и реально использовался для уменьшения cost'а в таблице
                                usedNotNulls = mNotNullProps.immutable().filter(rPushedProps.result);
                            assert bAdjStat.lessEquals(pushedStatKeys.getRows());//
                        }

                        bCostStat = new JoinCostStat<>(bv, pushedStatKeys, usedNotNulls);

                        if (rPushedKeys != null) {
                            pushedKeys = rPushedKeys.result; // теоретически можно и все ребра (в смысле что предикаты лишними не бывают ???)
                            pushedJoinStatKeys = pushedStatKeys;
                        }
                    }

                    // STAT ESTIMATE
                    Result<Stat> rAAdjStat = new Result<>(); Result<Stat> rBAdjStat = new Result<>();
                    Stat newStat = calcEstJoinStat(aAdjStat, bAdjStat, adjEdges, aEdgeStats, bEdgeStats, true, rAAdjStat, rBAdjStat);
                    aAdjStat = rAAdjStat.result; bAdjStat = rBAdjStat.result;
                    Cost newCost = aCost.or(bCostStat.getCost()).or(new Cost(newStat));

                    ImMap<BaseJoin, DistinctKeys> newKeyStats = aCostStat.getKeyStats().addExcl(bCostStat.getKeyStats());
                    ImMap<BaseJoin, Stat> newJoinStats = reduceIntermediateStats(newStat.min(aAdjStat), aCostStat).addExcl(reduceIntermediateStats(newStat.min(bAdjStat), bCostStat)); // также фильтруем по notNull
                    ImSet<BaseExpr> newUsedNotNulls = aCostStat.usedNotNulls.addExcl(bCostStat.usedNotNulls);

                    MMap<BaseExpr, Stat> mPropAdjStats = MapFact.mMap(minStat); // ключи не считаем, так как используются один раз. NotNull'ы не нужны, так как статистика уже редуцировалась
                    for (int k = 0, size = exprs.size(); k < size; k++) {
                        int i = exprs.getValue(k);
                        Stat aEdgeStat = aEdgeStats[i].min(aAdjStat);
                        Stat bEdgeStat = bEdgeStats[i].min(bAdjStat);

                        Stat propStat;
                        Stat keyStat;
                        if (aIsKeys[i]) {
                            keyStat = aEdgeStat;
                            propStat = bEdgeStat;
                        } else {
                            keyStat = bEdgeStat;
                            propStat = aEdgeStat;
                        }
                        if (keyStat.less(propStat))
                            mPropAdjStats.add(exprs.getKey(k), keyStat);
                    }
                    ImMap<BaseExpr, Stat> newPropStats = aCostStat.getPropStats().addExcl(bCostStat.getPropStats()).merge(mPropAdjStats.immutable(), minStat);

                    return new MergeCostStat(newCost, newStat,
                            bBaseCost, aAdjStat, bAdjStat, newJoinStats.size(),
                            newJoinStats, newKeyStats, newPropStats, pushedKeys, pushedJoinStatKeys, newUsedNotNulls,
                            aCostStat, bCostStat, aEdgeStats, bEdgeStats, edgesList);
                }
            };
    }

    // точка входа чтобы обозначить необходимую общую логику estimate'ов Push Cost (пока в Table) и Stat (в общем случае)
    public static Stat calcEstJoinStat(Stat aStat, Stat bStat, int edgesCount, Stat[] aEdgeStats, Stat[] bEdgeStats, boolean adjStat, Result<Stat> rAAdjStat, Result<Stat> rBAdjStat) {

        Stat totalStatReduce = Stat.ONE; // По умолчанию cost = MAX(a,b), stat = MAX((a + b)/(S(MAX(dist.from, dist.to))), MIN(a,b)), где dist.from = MIN(a.stat, e.dist.from), dist.to = MIN(b.stat, e.dist.to)

        Stat aReduce = Stat.ONE;
        Stat bReduce = Stat.ONE;
        for (int i = 0; i < edgesCount; i++) {
            Stat aEdgeStat = aEdgeStats[i].min(aStat);
            Stat bEdgeStat = bEdgeStats[i].min(bStat);

            if(aEdgeStat.less(bEdgeStat)) {
                bReduce = bReduce.max(bEdgeStat.div(aEdgeStat));
                totalStatReduce = totalStatReduce.mult(bEdgeStat);
            } else {
                aReduce = aReduce.max(aEdgeStat.div(bEdgeStat));
                totalStatReduce = totalStatReduce.mult(aEdgeStat);
            }
        }

        Stat aReducedStat;
        Stat bReducedStat;
        if(adjStat) {
            aReducedStat = aStat.div(aReduce);
            bReducedStat = bStat.div(bReduce);
            if(rAAdjStat != null) {
                rAAdjStat.set(aReducedStat);
                rBAdjStat.set(bReducedStat);
            }
        } else {
            aReducedStat = aStat;
            bReducedStat = bStat;
        }
        return (aStat.mult(bStat).div(totalStatReduce)).max(aReducedStat.min(bReducedStat));
    }

    private static ImMap<BaseJoin, Stat> reduceIntermediateStats(final Stat newStat, CostStat prevCost) {
        ImMap<BaseJoin, Stat> joinStats = prevCost.getJoinStats();
        if(prevCost.getStat().lessEquals(newStat))
            return joinStats; // не может измениться, так как и так меньше newStat

        return joinStats.mapValues(new GetValue<Stat, Stat>() {
            public Stat getMapValue(Stat value) {
                return value.min(newStat);
            }
        });
    }

    private Stat getStat(MAddMap<BaseJoin, Stat> joinStats, ImMap<Edge, Stat> edgeStats) {
        return getNodeStat(joinStats).div(getEdgeStat(joinStats, edgeStats));
    }

    private Cost getIndexedStat(MAddMap<BaseJoin, Cost> indexedStats, Stat finalStat) {
        Cost tableStat = new Cost(finalStat);
        for(int i=0,size=indexedStats.size();i<size;i++)
            if(indexedStats.getKey(i) instanceof InnerJoin)
                tableStat = tableStat.or(indexedStats.getValue(i));
        return tableStat;
    }

    private Stat getNodeStat(MAddMap<BaseJoin, Stat> nodeStats) {
        Stat rowStat = Stat.ONE;
        for(int i=0,size=nodeStats.size();i<size;i++)
            rowStat = rowStat.mult(nodeStats.getValue(i));
        return rowStat;
    }

    private <K extends BaseExpr> void buildBalancedGraph(ImSet<K> groups, final MAddMap<BaseJoin, Stat> joinStats, final MAddMap<BaseExpr, Stat> exprStats, Result<ImMap<Edge, Stat>> edgeStats, MAddMap<BaseJoin, Cost> indexedStats, StatType statType, KeyStat keyStat) {

        final MAddMap<Edge, Stat> keyStats = MapFact.mAddOverrideMap();

        Result<ImSet<Edge>> edges = new Result<>();
        MAddMap<BaseExpr, PropStat> propStats = MapFact.mAddOverrideMap();
        buildGraphWithStats(groups, edges, joinStats, propStats, keyStats, null, indexedStats, statType, keyStat, null);
        for(int i=0,size=propStats.size();i<size;i++)
            exprStats.add(propStats.getKey(i), propStats.getValue(i).distinct);

        balanceGraph(edges.result, joinStats, exprStats, keyStats, indexedStats);

        edgeStats.set(edges.result.mapValues(new GetValue<Stat, Edge>() {
            public Stat getMapValue(Edge value) {
                Stat propStat = value.getPropStat(joinStats, exprStats);
                assert propStat.equals(value.getKeyStat(joinStats, keyStats));
                return propStat;
            }}));
    }

    private static class Balancing implements Comparable<Balancing> {
        public final boolean key; // ключ или вершина
        public final Cost indexStat; // текущая индексная статистика
        public final Stat reduce; // степень уменьшения
        public final Stat reduceTo; // к чему уменьшаем

        public Balancing(boolean key, Cost indexStat, Stat reduce, Stat reduceTo) {
            this.key = key;
            this.indexStat = indexStat;
            this.reduce = reduce;
            this.reduceTo = reduceTo;
        }

        public final static Balancing MAX = new Balancing(false, null, null, null);

        private int statCompareTo(Balancing o) {
            int result;

            result = Integer.compare(indexStat.rows.getWeight(), o.indexStat.rows.getWeight());
            if(result != 0)
                return result; // min / max

            result = Integer.compare(reduce.getWeight(), o.reduce.getWeight());
            if(result != 0)
                return result; // min / max

            result = Integer.compare(reduceTo.getWeight(), o.reduceTo.getWeight());
            if(result != 0)
                return -result; // max / min

            return 0;
        }

        @Override
        public int compareTo(Balancing o) {
            if(this == MAX)
                return o == MAX ? 0 : 1;
            if(o == MAX)
                return -1;

//            int result;
//
//            result = costReduce.compareTo(o.costReduce);
//            if(result != 0)
//                return result;

            // агрессивность редуцирования
            int statCompare = statCompareTo(o);

//            if(costReduce.equals(CostReduce.NONE))
//                return -statCompare; // минимальная агрессивность, пытаемся вернуться к "индексированному" редуцированию
//            else
                return statCompare; // максимальная агрессивность, пытаемся по максимуму редуцировать "тяжелые" join'ы
        }
    }

    private static Balancing get(Edge edge, MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, Stat> exprStats, MAddMap<Edge, Stat> keyStats, MAddMap<BaseJoin, Cost> indexedStats) {
        Stat keys = edge.getKeyStat(joinStats, keyStats);
        Stat values = edge.getPropStat(joinStats, exprStats);

        if(keys.equals(values))
            return null;

        boolean key = values.less(keys);
        if(key)
            return new Balancing(true, indexedStats.get(edge.join), keys.div(values), values);
        else
            return new Balancing(false, indexedStats.get(edge.expr.getBaseJoin()), values.div(keys), keys);
    }

    private void balanceGraph(ImSet<Edge> edges, MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, Stat> exprStats, MAddMap<Edge, Stat> keyStats, MAddMap<BaseJoin, Cost> indexedStats) {

        // ищем несбалансированное ребро с максимальным costReduce, или
        Set<Edge> unbalancedEdges = SetFact.mAddRemoveSet(edges);

        while(unbalancedEdges.size() > 0) {
            Balancing bestBalancing = null; Edge<?> bestEdge = null;
            for(Edge edge : unbalancedEdges) {
                Balancing balancing = get(edge, joinStats, exprStats, keyStats, indexedStats);
                if(balancing != null) {
                    if (bestBalancing == null || balancing.compareTo(bestBalancing) < 0) { // если нашли новый минимум про старый забываем
                        bestBalancing = balancing;
                        bestEdge = edge;
                    }
                }
            }
            if(bestBalancing == null)
                break;

            BaseJoin decreaseJoin;
            Stat decrease = bestBalancing.reduce;
            if(!bestBalancing.key) { // балансируем значение
                decreaseJoin = bestEdge.expr.getBaseJoin();
                exprStats.add(bestEdge.expr, bestBalancing.reduceTo); // это и есть разница
            } else { // балансируем ключ, больше он использоваться не будет
                decreaseJoin = bestEdge.join;
                keyStats.add(bestEdge, bestBalancing.reduceTo);
            }
            joinStats.add(decreaseJoin, joinStats.get(decreaseJoin).div(decrease));

            if(indexedStats != null && decreaseJoin instanceof Table.Join && (bestBalancing.key || (bestEdge.expr.isIndexed() && !(bestEdge.join instanceof CalculateJoin))))
                indexedStats.add((Table.Join) decreaseJoin, indexedStats.get((Table.Join) decreaseJoin).div(decrease));
        }
    }

    private <K extends BaseExpr> void buildGraphWithStats(ImSet<K> groups, Result<ImSet<Edge>> edges, MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, PropStat> exprStats, MAddMap<Edge, Stat> keyStats,
                                                          MAddMap<BaseJoin, DistinctKeys> keyDistinctStats, MAddMap<BaseJoin, Cost> indexedStats, StatType statType, KeyStat keyStat, QueryJoin keepIdentJoin) {

        Result<ImSet<BaseExpr>> exprs = new Result<>();
        Result<ImSet<BaseJoin>> joins = new Result<>();

        buildGraph(groups, edges, exprs, joins, keyStat, statType, keepIdentJoin);

        // раньше было слияние expr'ов, которые входят в одни и те же join'ы, по идее это уменьшает кол-во двудольных графов и сильно помогает getMSTExCost
        // но если мы их сольем изначально, то (a1,b1) и (a2,b2) сольются в (a1 + a2, b1+b2) и мы можем потерять важную информацию, раньше же это делалось параллельно с балансировкой, но это очень сильно усложняло архитектуру и не вязалось с получением информации для pushDown'а
        // mergeCrossColumns();

        buildStats(joins, exprs, edges.result, joinStats, exprStats, keyStats, keyDistinctStats, indexedStats, statType, keyStat);
    }

    private void buildStats(Result<ImSet<BaseJoin>> joins, Result<ImSet<BaseExpr>> exprs, ImSet<Edge> edges, MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, PropStat> exprStats, MAddMap<Edge, Stat> keyStats, MAddMap<BaseJoin, DistinctKeys> keyDistinctStats, MAddMap<BaseJoin, Cost> indexedStats, final StatType statType, final KeyStat keyStat) {

        ImMap<BaseJoin, StatKeys> joinStatKeys = joins.result.mapValues(new GetValue<StatKeys, BaseJoin>() {
            public StatKeys getMapValue(BaseJoin value) {
                return value.getStatKeys(keyStat, statType, false);
            }});

        // читаем статистику по join'ам
        for(int i=0,size=joinStatKeys.size();i<size;i++) {
            BaseJoin<Object> join = joinStatKeys.getKey(i);
            StatKeys<Object> statKeys = joinStatKeys.getValue(i);
            joinStats.add(join, statKeys.getRows());
            indexedStats.add(join, statKeys.getCost());
            if(keyDistinctStats != null)
                keyDistinctStats.add(join, statKeys.getDistinct());
        }

        for(Edge edge : edges) {
            StatKeys<Object> statKeys = joinStatKeys.get(edge.join);
            if(keyStats != null)
                keyStats.add(edge, statKeys.getDistinct(edge.key));
        }

        // читаем статистику по значениям
        for(int i = 0, size = exprs.result.size(); i<size; i++) {
            BaseExpr expr = exprs.result.get(i);
            PropStat exprStat = expr.getStatValue(keyStat, statType);
            exprStats.add(expr, exprStat);
        }
    }

    private static void addQueueJoin(BaseJoin join, MSet<BaseJoin> mJoins, Queue<BaseJoin> queue, QueryJoin keepIdentJoin) {
        if(keepIdentJoin != null && BaseUtils.hashEquals(join, keepIdentJoin))
            join = keepIdentJoin;
        if(!mJoins.add(join))
            queue.add(join);
    }

    private List<WhereJoin> getAdjIntervalWheres(Result<UpWheres<WhereJoin>> upAdjWheres) {
        // в принципе в cost based это может быть не нужно, просто нужно сделать result cost и stat объединения двух ExprIndexedJoin = AverageIntervalStat и тогда жадняк сам разберется
        boolean hasExprIndexed = false; // оптимизация
        for(WhereJoin valueJoin : wheres)
            if(valueJoin instanceof ExprIndexedJoin) {
                hasExprIndexed = true;
                break;
            }
        if(!hasExprIndexed)
            return Arrays.asList(wheres);

        List<WhereJoin> result = new ArrayList<>();
        for(WhereJoin valueJoin : wheres)
            if(!(valueJoin instanceof ExprIndexedJoin))
                result.add(valueJoin);

        MMap<WhereJoin, UpWhere> mUpIntervalWheres = null;
        if(upAdjWheres != null)
            mUpIntervalWheres = MapFact.mMapMax(wheres.length, AbstractUpWhere.<WhereJoin>and());

        int intStat = Settings.get().getAverageIntervalStat();
        if(intStat >= 0)
            for(ExprIndexedJoin join : ExprIndexedJoin.getIntervals(wheres)) { // потом надо будет убрать intervals
                ExprStatJoin adjJoin = new ExprStatJoin(join.getJoins().singleValue(), new Stat(intStat, true));
                result.add(adjJoin);

                if(upAdjWheres != null)
                    mUpIntervalWheres.add(adjJoin, upAdjWheres.result.get(join));
            }

        if(upAdjWheres != null)
            upAdjWheres.set(new UpWheres<WhereJoin>(upAdjWheres.result.addExcl(mUpIntervalWheres.immutable())));

        return result;
    }

    private <K extends BaseExpr> void buildGraph(ImSet<K> groups, Result<ImSet<Edge>> edges, Result<ImSet<BaseExpr>> exprs, Result<ImSet<BaseJoin>> joins, KeyStat keyStat, StatType statType, QueryJoin keepIdentJoin) {
        MExclSet<Edge> mEdges = SetFact.mExclSet();
        MSet<BaseExpr> mExprs = SetFact.mSet();
        MSet<BaseJoin> mJoins = SetFact.mSet();
        Queue<BaseJoin> queue = new LinkedList<>();

        // собираем все ребра и вершины (без ExprIndexedJoin они все равно не используются при подсчете статистики, но с интервалами)
        for(WhereJoin valueJoin : getAdjIntervalWheres(null))
            addQueueJoin(valueJoin, mJoins, queue, keepIdentJoin);

        for(BaseExpr group : groups) {
            mExprs.add(group);
            addQueueJoin(group.getBaseJoin(), mJoins, queue, keepIdentJoin);
        }
        while(!queue.isEmpty()) {
            BaseJoin<Object> join = queue.poll();
            ImMap<?, BaseExpr> joinExprs = getJoinsForStat(join);

            for(int i=0,size=joinExprs.size();i<size;i++) {
                Object joinKey = joinExprs.getKey(i);
                BaseExpr joinExpr = joinExprs.getValue(i);

                Edge edge = new Edge(join, joinKey, joinExpr);
                mEdges.exclAdd(edge);
                mExprs.add(joinExpr);

                addQueueJoin(joinExpr.getBaseJoin(), mJoins, queue, keepIdentJoin);
            }
        }
        exprs.set(mExprs.immutable());

        // добавляем notNull статистику
        for(Expr expr : exprs.result) {
            if(expr instanceof InnerExpr) {
                InnerExpr innerExpr = (InnerExpr) expr;
                ExprStatJoin notNullJoin = innerExpr.getNotNullJoin(keyStat, statType);
                if (notNullJoin != null && !mJoins.add(notNullJoin))
                    mEdges.exclAdd(new Edge(notNullJoin, 0, innerExpr));
            }
        }

        joins.set(mJoins.immutable());
        edges.set(mEdges.immutable());
    }

    private Stat getEdgeStat(MAddMap<BaseJoin, Stat> joinStats, ImMap<Edge, Stat> edgeStats) {
        // высчитываем total

        int pessStatType = Settings.get().getPessStatType();
        
        Stat total = null;
        
        if(pessStatType != 3) {
            total = Stat.ONE;
            for (Stat stat : edgeStats.valueIt()) {
                total = total.mult(stat);
            }
        }

        if(pessStatType == 0)
            return total;

        Stat mt = null;
        // multi tree stat
        if(pessStatType != 3) {
            mt = getMTCost(joinStats, edgeStats, total);
            assert mt.lessEquals(total);
            
            if (pessStatType == 1)
                return mt;
        }

        // minimum spanning tree cost
        Stat mst = getMSTExCost(joinStats, edgeStats);
        if(pessStatType == 3)
            return mst;
        
        assert mst.lessEquals(mt) && pessStatType == 2;        
        return mst.avg(mt);
    }

    private Stat getMSTCost(MAddMap<BaseJoin, Stat> joinStats, MAddExclMap<BaseExpr, Set<Edge>> balancedEdges, MAddExclMap<BaseExpr, Stat> balancedStats) {
        UndirectedGraph<BaseJoin> graph = new UndirectedGraph<>();
        BaseJoin root = ValueJoin.instance; // чтобы создать связность
        graph.addNode(root);
        for(int i=0,size=joinStats.size();i<size;i++) {
            BaseJoin node = joinStats.getKey(i);
            graph.addNode(node);
            graph.addEdge(root, node, 0);
        }
        
        for(int i=0;i<balancedEdges.size();i++) {
            BaseExpr bExpr = balancedEdges.getKey(i);
            BaseJoin from = bExpr.getBaseJoin();
            for(Edge edge : balancedEdges.getValue(i)) {
                assert BaseUtils.hashEquals(edge.expr, bExpr);
                graph.addEdge(from, edge.join, - balancedStats.get(bExpr).getWeight());
            }
        }
        return new Stat(-Prim.mst(graph).calculateTotalEdgeCost(), true);
    }

    private Stat getMSTExCost(MAddMap<BaseJoin, Stat> nodeStats, ImMap<Edge, Stat> edgeStats) {

        SpanningTreeWithBlackjack<BaseJoin> graph = new SpanningTreeWithBlackjack<>();
        for(int i=0,size=nodeStats.size();i<size;i++) {
            BaseJoin node = nodeStats.getKey(i);
            graph.addNode(node, node.getJoins().isEmpty() ? 0 : nodeStats.getValue(i).getWeight());
        }
        for(int i=0;i<edgeStats.size();i++) {
            Edge edge = edgeStats.getKey(i);
            graph.addEdge(edge.expr.getBaseJoin(), edge.join, edgeStats.getValue(i).getWeight());
        }

        int maxIterations = Settings.get().getMaxEdgeIterations();
        return new Stat(graph.calculate(BaseUtils.max(edgeStats.size() - nodeStats.size(), 1) * maxIterations), true);
    }

    private Stat getMTCost(MAddMap<BaseJoin, Stat> joinStats, ImMap<Edge, Stat> edgeStats, Stat totalBalanced) {

        ImMap<BaseExpr, ImSet<Edge>> outEdges = edgeStats.keys().group(new BaseUtils.Group<BaseExpr, Edge>() {
            public BaseExpr group(Edge value) {
                return value.expr;
            }});

        MExclMap<BaseJoin, MExclSet<Edge>> mEdges = MapFact.mExclMap();
        for(int i=0,size=joinStats.size();i<size;i++) {
            mEdges.exclAdd(joinStats.getKey(i), SetFact.<Edge>mExclSet());
        }
        for(int i=0;i<outEdges.size();i++) {
            BaseExpr bExpr = outEdges.getKey(i);
            BaseJoin from = bExpr.getBaseJoin();
            MExclSet<Edge> mFromEdges = mEdges.get(from);
            for(Edge edge : outEdges.getValue(i)) {
                assert BaseUtils.hashEquals(edge.expr, bExpr);
                mFromEdges.exclAdd(edge);
            }            
        }
        ImMap<BaseJoin, ImSet<Edge>> edges = MapFact.immutable(mEdges);
        Pair<Integer, ImSet<Edge>> mt = recBuildMT(MapFact.buildGraphOrder(edges, new GetValue<BaseJoin, Edge>() {
            public BaseJoin getMapValue(Edge value) {
                return value.join;
            }}), edges, edgeStats.fnGetValue(), new HashSet<Edge>(), 0, new HashMap<BaseJoin, ImMap<BaseJoin, Edge>>(), 0, null);
        if(mt == null)
            return totalBalanced;
        return totalBalanced.div(new Stat(mt.first, true));
    }
    
    // proceeded - из какой вершины в какую можно пройти и вершина через которую надо идти
    private Pair<Integer, ImSet<Edge>> recBuildMT(ImOrderSet<BaseJoin> order, ImMap<BaseJoin, ImSet<Edge>> edgesOuts, final GetValue<Stat, Edge> edgeStats, Set<Edge> removedEdges, int removedStat, Map<BaseJoin, ImMap<BaseJoin, Edge>> currentTree, int currentIndex, Pair<Integer, ImSet<Edge>> currentMin) {
        if(currentIndex >= order.size()) {
            return new Pair<>(removedStat, SetFact.fromJavaSet(removedEdges));
        }
        
        BaseJoin currentNode = order.get(currentIndex);
        ImSet<Edge> edgesOut = edgesOuts.get(currentNode);
        
        MExclMap<BaseJoin, Edge> edgeOutTree = MapFact.mExclMap();
        for(Edge edgeOut : edgesOut) {
            if(removedEdges.contains(edgeOut)) // избыточная проверка с точки зрения того что removedEdges содержит уже отработанные node'ы
                continue;
            ImMap<BaseJoin, Edge> reachableEdges = currentTree.get(edgeOut.join).addExcl(edgeOut.join, edgeOut);
            // нашли "два пути", edge на одном из путей надо вырезать рекурсивно выбираем минимум
            for(int i=0,size=reachableEdges.size();i<size;i++) {
                BaseJoin reachableJoin = reachableEdges.getKey(i);
//                Edge reachableEdge = reachableEdges.getValue(i);

                Edge presentEdgeOut = edgeOutTree.get(reachableJoin);
                if(presentEdgeOut != null) { // нашли цикл, один через edgeOut, второй через presentEdgeOut, один из edge'й на этих путях придется удалить в любом случае (это и перебираем)
                    // бежим по обоим найденным путям, упорядочив по минимальным весам 
                    Iterable<Edge> edges = BaseUtils.sort(BaseUtils.mergeIterables(getEdgePath(currentTree, edgeOut, reachableJoin), getEdgePath(currentTree, presentEdgeOut, reachableJoin)), new Comparator<Edge>() {
                        public int compare(Edge o1, Edge o2) {
                            return Integer.compare(edgeStats.getMapValue(o1).getWeight(), edgeStats.getMapValue(o2).getWeight());
                        }});
                    for(Edge currentEdge : edges) {
                        // пробуем удалить ребро
                        int newRemovedStat = removedStat + edgeStats.getMapValue(currentEdge).getWeight();
                        if(currentMin == null || currentMin.first > newRemovedStat) {
                            MAddExclMap<BaseJoin, ImMap<BaseJoin, Edge>> stackRemoved = removeEdge(currentTree, currentEdge);
                            removedEdges.add(currentEdge);
                            Pair<Integer, ImSet<Edge>> recCut = recBuildMT(order, edgesOuts, edgeStats, removedEdges, newRemovedStat, currentTree, currentIndex, currentMin);// придется начинать с 0 чтобы перестроить дерево
                            removedEdges.remove(currentEdge);
                            MapFact.addJavaAll(currentTree, stackRemoved);

                            if(recCut != null) {
                                assert currentMin == null || recCut.first < currentMin.first || recCut == currentMin;
                                currentMin = recCut;
                            }
                        }
                    }
                    return currentMin;
                } else
                    edgeOutTree.exclAdd(reachableJoin, edgeOut);                
            }
        }
        currentTree.put(currentNode, edgeOutTree.immutable()); // assert что не было
        Pair<Integer, ImSet<Edge>> result = recBuildMT(order, edgesOuts, edgeStats, removedEdges, removedStat, currentTree, currentIndex + 1, currentMin);
        currentTree.remove(currentNode); // assert что было
        return result;
    }
    
    private Iterable<Edge> getEdgePath(final Map<BaseJoin, ImMap<BaseJoin, Edge>> currentTree, final Edge startEdge, final BaseJoin endNode) {
        return new Iterable<Edge>() {
            public Iterator<Edge> iterator() {
                return new Iterator<Edge>() {
                    Edge currentEdge = startEdge; 

                    public boolean hasNext() {
                        return currentEdge != null;
                    }

                    public Edge next() {
                        Edge nextEdge = currentEdge;
                        currentEdge = currentTree.get(currentEdge.join).get(endNode);
                        assert currentEdge != null || BaseUtils.hashEquals(nextEdge.join, endNode);
                        return nextEdge;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    // удаляет из дерева ребро
    private MAddExclMap<BaseJoin, ImMap<BaseJoin, Edge>> removeEdge(Map<BaseJoin, ImMap<BaseJoin, Edge>> currentTree, Edge removeEdge) {
        BaseJoin to = removeEdge.join;
        MAddExclMap<BaseJoin, ImMap<BaseJoin, Edge>> rest = MapFact.mAddExclMap();
        final BaseJoin from = removeEdge.expr.getBaseJoin();
        final ImSet<BaseJoin> toNodes = currentTree.get(to).keys().addExcl(to);
        for(Map.Entry<BaseJoin, ImMap<BaseJoin, Edge>> entry : currentTree.entrySet()) {
            BaseJoin node = entry.getKey();
            ImMap<BaseJoin, Edge> nodes = entry.getValue();
            if(nodes.containsKey(from) || BaseUtils.hashEquals(node, from)) {
                rest.exclAdd(entry.getKey(), nodes);
                entry.setValue(nodes.removeIncl(toNodes));
            }
        }
        return rest;
    }

    private static class PushResult {
        private final Stat runStat;

        private final List<WhereJoin> joins;
        private final UpWheres<WhereJoin> upWheres;

        public PushResult(Stat runStat, List<WhereJoin> joins, UpWheres<WhereJoin> upWheres) {
            this.runStat = runStat;

            this.joins = joins;
            this.upWheres = upWheres;
        }

        private <T extends Expr> Where getWhere(ImMap<T, ? extends Expr> translate) {
            Where result = Where.TRUE;
            for (WhereJoin join : joins)
                result = result.and(getUpWhere(join, upWheres.get(join))); // чтобы не потерять or, правда при этом removeJoin должен "соответствовать" не TRUE calculateOrWhere

            return GroupExpr.create(translate, result, translate.keys().toMap()).getWhere();
        }
    }

    private static Where getUpWhere(WhereJoin join, UpWhere upWhere) {
        return upWhere.getWhere().and(BaseExpr.getOrWhere(join));
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, final KeyStat keyStat, StatType type, final KeyEqual keyEqual) {
        if(!keyEqual.isEmpty()) { // для оптимизации
            return and(keyEqual.getWhereJoins()).getStatKeys(groups, keyEqual.getKeyStat(keyStat), type);
        } else
            return getStatKeys(groups, keyStat, type);
    }

    public static <T extends WhereJoin> WhereJoins removeJoin(QueryJoin removeJoin, WhereJoin[] wheres, UpWheres<WhereJoin> upWheres, Result<UpWheres<WhereJoin>> resultWheres) {
        WhereJoins result = null;
        UpWheres<WhereJoin> resultUpWheres = null;
        MExclSet<WhereJoin> mKeepWheres = SetFact.mExclSetMax(wheres.length); // массивы
        for(WhereJoin whereJoin : wheres) {
            WhereJoins removeJoins;
            Result<UpWheres<WhereJoin>> removeUpWheres = new Result<>();

            boolean remove = BaseUtils.hashEquals(removeJoin, whereJoin);
            InnerJoins joinFollows = null; Result<UpWheres<InnerJoin>> joinUpWheres = null;
            if (!remove && whereJoin instanceof ExprStatJoin && ((ExprStatJoin) whereJoin).depends(removeJoin)) // без этой проверки может бесконечно проталкивать
                remove = true;
            if (!remove && whereJoin instanceof ExprIndexedJoin && ((ExprIndexedJoin)whereJoin).givesNoKeys()) // даст висячий ключ при проталкивании, вообще рекурсивно пойти не может, но смысла нет разбирать
                remove = true;
            // нижние проверки должны соответствовать calculateOrWhere 
            if(!remove && whereJoin instanceof PartitionJoin) {
                if(UnionJoin.depends(((PartitionJoin) whereJoin).getOrWhere(), removeJoin))
                    remove = true;
            }
            if(!remove) {
                Result<ImSet<UnionJoin>> unionJoins = new Result<>();
                joinUpWheres = new Result<>();
                joinFollows = whereJoin.getJoinFollows(joinUpWheres, unionJoins);
                for(UnionJoin unionJoin : unionJoins.result) // без этой проверки может бесконечно проталкивать
                    if(unionJoin.depends(removeJoin)) {
                        remove = true;
                        break;
                    }
            }

            if(remove) {
                removeJoins = WhereJoins.EMPTY;
                removeUpWheres.set(UpWheres.<WhereJoin>EMPTY());
            } else
                removeJoins = joinFollows.removeJoin(removeJoin,
                        BaseUtils.<UpWheres<WhereJoin>>immutableCast(joinUpWheres.result), removeUpWheres);

            if(removeJoins!=null) { // вырезали, придется выкидывать целиком join, оставлять sibling'ом
                if(result==null) {
                    result = removeJoins;
                    resultUpWheres = removeUpWheres.result;
                } else {
                    result = result.and(removeJoins);
                    resultUpWheres = result.andUpWheres(resultUpWheres, removeUpWheres.result);
                }
            } else
                mKeepWheres.exclAdd(whereJoin);
        }

        if(result!=null) {
            ImSet<WhereJoin> keepWheres = mKeepWheres.immutable();
            result = result.and(new WhereJoins(keepWheres));
            resultWheres.set(result.andUpWheres(resultUpWheres, upWheres.filterUp(keepWheres)));
            return result;
        }
        return null;
    }

    // устраняет сам join чтобы при проталкивании не было рекурсии
    public WhereJoins removeJoin(QueryJoin join, UpWheres<WhereJoin> upWheres, Result<UpWheres<WhereJoin>> resultWheres) {
        return removeJoin(join, wheres, upWheres, resultWheres);
    }

    public <K extends BaseExpr> WhereJoins pushStatKeys(StatKeys<K> statKeys) {
        if(statKeys == StatKeys.<K>NOPUSH())
            return this;
        return and(new WhereJoins(new StatKeysJoin<>(statKeys)));
    }

    // получает подможнство join'ов которое дает joinKeys, пропуская skipJoin. тут же алгоритм по определению достаточных ключей
    // !!! ТЕОРЕТИЧЕСКИ НЕСМОТРЯ НА REMOVE из-за паковки может проталкивать бесконечно (впоследствии нужен будет GUARD), например X = L(G1 + G2) AND (G1 OR G2) спакуется в X = L(G1 + G2) AND (G1' OR G2) , (а не L(G1' + G2), и будет G1 проталкивать бесконечно)
    //  но это очень редкая ситуация и важно проследить за ее природой, так как возможно есть аналогичные assertion'ы
    // может неправильно проталкивать в случае если скажем есть документы \ строки, строки "материализуются" и если они опять будут группироваться по документу, информация о том что он один уже потеряется
    public <K extends Expr, T extends Expr> Where getPushWhere(ImMap<K, BaseExpr> joinMap, UpWheres<WhereJoin> upWheres, QueryJoin<K, ?, ?, ?> pushJoin, boolean isInner, KeyStat keyStat, Where fullWhere, StatKeys<K> currentJoinStat) {
        // joinKeys из skipJoin.getJoins()

//        Where costResult = getWhereJoins(pushJoin, isInner).getCostPushWhere(pushJoin, upWheres, keyStat, StatType.PUSH_OUTER());
//        Where oldResult = getOldPushWhere(joinMap, upWheres, pushJoin, keyStat, fullWhere, currentJoinStat);
//
//        if(!BaseUtils.nullHashEquals(costResult, oldResult))
//            costResult = costResult;
//
//        if(1==1) return costResult;

        if(useCost)
            return getWhereJoins(pushJoin, isInner).getCostPushWhere(pushJoin, upWheres, keyStat, StatType.PUSH_OUTER());
        else
            return getOldPushWhere(joinMap, upWheres, pushJoin, keyStat, fullWhere, currentJoinStat);
    }

    private <K extends Expr> WhereJoins getWhereJoins(QueryJoin<K, ?, ?, ?> pushJoin, boolean isInner) {
        if(isInner) {
            if(pushJoin.isValue()) // проблема что queryJoin может быть в ExprStatJoin.valueJoins, тогда он будет Inner, а в WhereJoins его не будет и начнут падать assertion'ы появлятся висячие ключи, другое дело, что потом надо убрать в EqualsWhere ExprStatJoin = значение, тогда это проверка не нужно
                return new WhereJoins(pushJoin);
            return this;
        }
        return and(new WhereJoins(pushJoin));
    }

    private <K extends Expr> Where getOldPushWhere(ImMap<K, BaseExpr> joinMap, UpWheres<WhereJoin> upWheres, QueryJoin<K, ?, ?, ?> pushJoin, KeyStat keyStat, Where fullWhere, StatKeys<K> currentJoinStat) {
        assert joinMap.equals(pushJoin.getJoins().filterIncl(joinMap.keys()));
        Result<UpWheres<WhereJoin>> upFitWheres = new Result<>();
        WhereJoins removedJoins = removeJoin(pushJoin, upWheres, upFitWheres);
        if(removedJoins==null) {
            removedJoins = this;
            upFitWheres.set(upWheres);
        }

        return removedJoins.getPushWhere(joinMap, keyStat, StatType.PUSH_OUTER(), fullWhere.getStatRows(StatType.PUSH_INNER()), currentJoinStat, upFitWheres.result, pushJoin);
    }

    private static class PushJoinResult<K extends Expr> {
        private final PushResult joins;
        private final ImMap<K, BaseExpr> group;

        public PushJoinResult(PushResult joins, ImMap<K, BaseExpr> group) {
            this.joins = joins;
            this.group = group;
        }
    }

    private <K extends Expr> Where getPushWhere(ImMap<K, BaseExpr> joinMap, KeyStat keyStat, StatType type, Stat currentStat, StatKeys<K> currentJoinStat, UpWheres<WhereJoin> upWheres, QueryJoin<K, ?, ?, ?> pushJoin) {
        assertJoinRowStat(currentStat, currentJoinStat);

        final PushJoinResult<K> pushResult = getPushJoins(joinMap, keyStat, type, currentStat, currentJoinStat, upWheres);
        if(pushResult == null)
            return null;
        assert BaseUtils.hashEquals(pushJoin.getJoins().filterIncl(pushResult.group.keys()), pushResult.group);
        return pushResult.joins.getWhere(pushJoin.getPushGroup(pushResult.group, false, null));
    }

    private <K extends Expr, T extends Expr> PushJoinResult<K> getPushJoins(ImMap<K, BaseExpr> joinMap, KeyStat keyStat, StatType type, Stat currentStat, StatKeys<K> currentJoinStat, UpWheres<WhereJoin> upWheres) {
        PushJoinResult<K> pushResult;
        Stat baseStat = currentStat.min(Stat.ALOT);
        pushResult = getPushJoins(joinMap, keyStat, type, currentStat, currentJoinStat, upWheres, baseStat);
        if(pushResult == null)
            return null;
        return pushResult;
    }

    // как правило работает, но в каких то очень редких случаях вроде синхронизации нет, надо будет потом разобраться, пока не критично
    private <K extends Expr> void assertJoinRowStat(Stat currentStat, StatKeys<K> currentJoinStat) {
//        assert currentJoinStat.rows.equals(StatKeys.create(currentStat, currentJoinStat.distinct).rows);
    }

    private <K extends Expr, T extends Expr> PushJoinResult<K> getPushJoins(ImMap<K, BaseExpr> innerOuter, KeyStat keyStat, StatType type, Stat innerRows, StatKeys<K> innerKeys, UpWheres<WhereJoin> upWheres, Stat baseStat) {
        Result<ImRevMap<K, BaseExpr>> revInnerOuter = new Result<>();
        innerKeys = innerKeys.toRevMap(innerOuter, revInnerOuter);

        // считаем начальную итерацию, вырезаем WhereJoins которые "входят" в group
        final ImSet<ParamExpr> keepKeys = SetFact.<ParamExpr>EMPTY();
        Comparator<PushElement> comparator = getComparator(keepKeys);
        final ImSet<PushGroup<K>> groups = revInnerOuter.result.mapSetValues(new GetKeyValue<PushGroup<K>, K, BaseExpr>() {
            public PushGroup<K> getMapValue(K key, BaseExpr value) {
                return new PushGroup<>(key, value);
            }});
        List<PushElement> newPriority = new ArrayList<>();
        MExclSet<PushElement> mNewElements = SetFact.<PushElement>mExclSet(groups);
        MExclSet<WhereJoin> mNewJoins = SetFact.mExclSet();
        for(PushGroup<K> group : groups)
            BaseUtils.addToOrderedList(newPriority, group, 0, comparator);
        addJoins(Arrays.asList(wheres), upWheres, comparator, newPriority, groups, mNewElements, mNewJoins, null);
        final PushIteration<K> initialIteration = new PushIteration<>(mNewElements.immutable(), mNewJoins.immutable(), revInnerOuter.result, keepKeys, newPriority);

        // перебираем
        Result<BestResult> rBest = new Result<BestResult>(new BaseStat(baseStat));
        recPushJoins(initialIteration, keyStat, type, innerRows, innerKeys, PushIteration.Reduce.NONE, rBest, false);

        if(rBest.result instanceof BaseStat) // не нашли ничего лучше
            return null;
        final PushIteration<K> best = (PushIteration<K>) rBest.result;

        MMap<WhereJoin, UpWhere> bestUpWheres = MapFact.mMap(MapFact.<WhereJoin, UpWhere>override());
        for(PushElement element : best.elements)
            if(element instanceof PushJoin) {
                PushJoin join = (PushJoin)element;
                bestUpWheres.add(join.join, join.upWhere);
            }
        return new PushJoinResult<>(new PushResult(best.estStat, best.joins.toList().toJavaList(), new UpWheres<>(bestUpWheres.immutable())), best.innerOuter);
    }

    private static abstract class PushElement {

        protected abstract OuterContext<?> getOuterContext();
        protected abstract BaseJoin<?> getBaseJoin();

        public boolean containsAll(WhereJoin join) {
            return containsJoinAll(getBaseJoin(), join);
        }

        private InnerJoins getJoinFollows(Result<UpWheres<InnerJoin>> upWheres) {
            return InnerExpr.getJoinFollows(getBaseJoin(), upWheres, null);
        }

        public long getComplexity() {
            return getOuterContext().getComplexity(false);
        }

        public ImSet<ParamExpr> getKeys() {
            return getOuterContext().getOuterKeys();
        }
    }

    private static class PushJoin extends PushElement {

        private final WhereJoin join;
        private final UpWhere upWhere;

        public PushJoin(WhereJoin join, UpWhere upWhere) {
            this.join = join;
            this.upWhere = upWhere;
        }

        protected OuterContext<?> getOuterContext() {
            return join;
        }

        protected BaseJoin<?> getBaseJoin() {
            return join;
        }
    }

    private static class PushGroup<K extends Expr> extends PushElement {
        private final K inner;
        private final BaseExpr outer;

        public PushGroup(K inner, BaseExpr outer) {
            this.inner = inner;
            this.outer = outer;
        }

        protected OuterContext<?> getOuterContext() {
            return outer;
        }

        protected BaseJoin<?> getBaseJoin() {
            return outer.getBaseJoin();
        }
    }

    private static int getGroupPriority(PushElement o1) {
        return o1 instanceof PushGroup ? 0 : 1; // group'ы лучше
    }

    private static int getKeysPriority(PushElement o1, ImSet<ParamExpr> keepKeys) {
        return -o1.getKeys().remove(keepKeys).size(); // чем больше не keep ключей тем лучше
    }


    public static Comparator<PushElement> getComparator (final ImSet<ParamExpr> keepKeys) {
        return new Comparator<PushElement>() {
            public int compare(PushElement o1, PushElement o2) {
                // группы
                int compare = Integer.compare(getGroupPriority(o1), getGroupPriority(o2));
                if(compare != 0)
                    return compare;

                // количество "свободных" (не keep) ключей, чтобы быстрее отсечения получить
                compare = Integer.compare(getKeysPriority(o1, keepKeys), getKeysPriority(o2, keepKeys));
                if(compare != 0)
                    return compare;

                // наименьшая "сложность" join'ов, чтобы отсечение лучше было
                return Long.compare(o1.getComplexity(), o2.getComplexity());
            }
        };
    }

    private static abstract class BestResult {

        protected Stat estStat;
        protected abstract long getSecPriority();

        // обе должны быть убывающими при вырезании join'ов, без изменения PRIM
        protected boolean primBetter(BestResult iteration) { // если лучше, то при remove'е join'ов не убирая ключи или группы результат не улучшишь
            return estStat.less(iteration.estStat);
        }
        protected boolean secBetter(BestResult iteration) {
            return getSecPriority() < iteration.getSecPriority();
        }
    }

    private static class BaseStat extends BestResult {

        public BaseStat(Stat baseStat) {
            estStat = baseStat;
        }

        protected long getSecPriority() { // мнтересует только если статистика строго меньше
            return Long.MIN_VALUE;
        }
    }

    private static class PushIteration<K extends Expr> extends BestResult {

        private final ImSet<PushElement> elements; // для исключения избыточных WhereJoin
        private final ImSet<WhereJoin> joins;
        private final ImRevMap<K, BaseExpr> innerOuter;
        private final ImSet<ParamExpr> keepKeys;
        private final List<PushElement> priority; // только не keep в порядке компаратора

        public PushIteration(ImSet<PushElement> elements, ImSet<WhereJoin> joins, ImRevMap<K, BaseExpr> innerOuter, ImSet<ParamExpr> keepKeys, List<PushElement> priority) {
            this.elements = elements;
            this.joins = joins;
            this.innerOuter = innerOuter;
            this.keepKeys = keepKeys;
            this.priority = priority;

            assert checkPriority(priority, getComparator(keepKeys));
        }

        private WhereJoins getJoins() {
            return new WhereJoins(joins);
        }
        private ImRevMap<K, BaseExpr> getInnerOuter() {
            return innerOuter;
        }

        public boolean hasElement() {
            return !priority.isEmpty();
        }

        public PushElement findElement() {
            return priority.get(0);
        }

        private static <J> Stat calcMultStat(ImSet<J> groups, Stat rows, StatKeys<J> keys) {
            Stat sum = Stat.ONE;
            for(J group : groups)
                sum = sum.mult(keys.getDistinct(group));
            return sum.min(rows);
        }

        // MAX(Ri * MIN (*(Jo), Ro) / MIN(*(Ji), Ri)  , Ro)
        private static <K> Stat calcEstStat(ImRevMap<K, BaseExpr> innerOuter, Stat outerRows, StatKeys<BaseExpr> outerKeys, Stat innerRows, StatKeys<K> innerKeys) {
            return innerRows.mult(calcMultStat(innerOuter.valuesSet(), outerRows, outerKeys)).div(calcMultStat(innerOuter.keys(), innerRows, innerKeys)).max(outerRows);
        }
        private static <K> int calcIndexDecrease(ImRevMap<K, BaseExpr> innerOuter, Stat outerRows, StatKeys<BaseExpr> outerKeys, Stat innerRows, StatKeys<K> innerKeys) {
            Stat sum = Stat.ONE;
            for(int i=0,size=innerOuter.size();i<size;i++){
                K inner = innerOuter.getKey(i);
                BaseExpr outer = innerOuter.getValue(i);

                Stat innerStat = innerKeys.getDistinct(inner);
                Stat outerStat = outerKeys.getDistinct(outer);
                if(outerStat.less(innerStat) && inner instanceof BaseExpr && ((BaseExpr) inner).isIndexed()) {
                    sum = sum.mult(innerStat.div(outerStat));
                }
            }
            return sum.getWeight();
        }

        private void calcEstStat(Stat innerRows, StatKeys<K> innerKeys, KeyStat keyStat, StatType type) {
            ImRevMap<K, BaseExpr> innerOuter = getInnerOuter();
            WhereJoins joins = getJoins();

            Result<Stat> rows = new Result<>();
            StatKeys<BaseExpr> statKeys = joins.getStatKeys(innerOuter.valuesSet(), rows, keyStat, type);

            estStat = calcEstStat(innerOuter, rows.result, statKeys, innerRows, innerKeys);
            indexDecrease = calcIndexDecrease(innerOuter, rows.result, statKeys, innerRows, innerKeys);
        }

        private int indexDecrease;

        private long calcSecPriority() {
            long result = 0;
            for(WhereJoin element : joins)
                result += element.getComplexity(false);
            for(BaseExpr expr : innerOuter.valueIt())
                result += expr.getComplexity(false);
            return result;// - 1000 * indexDecrease;
        }

        protected Long secPriority;
        @ManualLazy
        protected long getSecPriority() {
            if(secPriority == null)
                secPriority = calcSecPriority();
            return secPriority;
        }

        private enum Reduce {
            PRIM, // Stat, group keys + keyExprs
            SEC, // PRIM (>=)= best ищем уменьшение SEC (если конечно не reducePrim)
            NONE
        }

        public Reduce checkBest(Reduce forceReduce, Result<BestResult> bestIteration, Stat innerRows, StatKeys<K> innerKeys, KeyStat keyStat, StatType type) { // возвращает если заведомо хуже best
            if(forceReduce == Reduce.PRIM) // если REDUCE.PRIM то ничего не проверяем
                return forceReduce;

            if(forceReduce == Reduce.SEC) { // если ждем reduce'а вторичного признака, не считаем estStat до того как проверим вторичный признак (но считать estStat все равно придется, чтобы не увеличить его случайно)
                assert bestIteration.result != null; // так как опция Reduce.SEC может включится только при равенстве Redisce.PRIM
                if(!secBetter(bestIteration.result))
                    return forceReduce;
            }

            // считаем estStat
            calcEstStat(innerRows, innerKeys, keyStat, type);

            // если best меньше
            if(bestIteration.result != null && bestIteration.result.primBetter(this)) // помечаем что мы должны убрать кдюч, так как если мы уберем этот join, то join outer и row outer, а значит и runstat вырастут
                return Reduce.PRIM;

            // если текущая меньше
            if(bestIteration.result == null || forceReduce == Reduce.SEC || primBetter(bestIteration.result) || secBetter(bestIteration.result)) // 2-я проверка - оптимизация
                bestIteration.set(this); // здесь и внизу SEC не нужен, так как он ASSERT'ся и так

            return Reduce.SEC;
        }

        public boolean hasNoReducePrim() { // оптимизация, если нет группировок и не keep ключей
            final PushElement element = findElement();

            // оптимизация завязана на реализацию findElement, assert что есть упорядочивание
            return !(element instanceof PushGroup) && keepKeys.containsAll(element.getKeys());
        }

        public PushIteration<K> keepElement() {
            final PushElement element = findElement();

            List<PushElement> newPriority = new ArrayList<>(priority);
            newPriority.remove(0);

            // инкрементально пересчитываем кэши (keepkeys + priority)
            ImSet<ParamExpr> addKeepKeys = element.getKeys().remove(this.keepKeys);

            if(addKeepKeys.isEmpty()) // оптимизация
                return new PushIteration<>(elements, joins, innerOuter, keepKeys, newPriority);

            ImSet<ParamExpr> newKeepKeys = this.keepKeys.addExcl(addKeepKeys);

            MAddSet<PushElement> validateElements = SetFact.mAddSet();
            Comparator<PushElement> comparator = getComparator(newKeepKeys);
            for(int i=1,size=priority.size();i<size;i++) { // обновляем priority с учетом изменения comparator\а
                final PushElement rest = priority.get(i);
                final ImSet<ParamExpr> restKeys = rest.getKeys();
                if(restKeys.intersect(addKeepKeys)) { // если пересекаются ключи, выкидываем, добавляем еще раз
                    newPriority.remove(rest);
//                    BaseUtils.addToOrderedList(newPriority, rest, 0, comparator);
                    validateElements.add(rest);
                }

                if(rest instanceof PushJoin && this.keepKeys.containsAll(restKeys)) // оптимизация по comparator'у в priority, если все из keep выходим
                    break;
            }

            for(PushElement validateElement : validateElements)
                BaseUtils.addToOrderedList(newPriority, validateElement, 0, comparator);

            return new PushIteration<>(elements, joins, innerOuter, newKeepKeys, newPriority);
        }

        private boolean checkPriority(List<PushElement> priority, Comparator<PushElement> comparator) {
            List<PushElement> checkPriority = new ArrayList<>();
            for(PushElement element : priority)
                BaseUtils.addToOrderedList(checkPriority, element, 0, comparator);
            boolean result = checkPriority.equals(priority);
            assert result;
            return result;
        }

        public PushIteration<K> removeElement(Result<Boolean> reducedPrim) { // group не просто вырезаем а заменяем на join с upWhere
            PushElement element = findElement();

            List<PushElement> newPriority = new ArrayList<>(priority);
            newPriority.remove(0);

            final ImSet<PushElement> removedElements = elements.removeIncl(element);
            MExclSet<PushElement> mNewElements = SetFact.mExclSet(removedElements);

            MExclSet<WhereJoin> mNewJoins; ImRevMap<K, BaseExpr> newInnerOuter; Set<ParamExpr> removedKeys;
            if(element instanceof PushGroup) {
                mNewJoins = SetFact.mExclSet(joins);
                newInnerOuter = innerOuter.removeRev(((PushGroup<K>) element).inner);
                removedKeys = null;
            } else {
                mNewJoins = SetFact.mExclSet(joins.removeIncl(((PushJoin) element).join));
                newInnerOuter = innerOuter;
                removedKeys = SetFact.mAddRemoveSet(element.getKeys().remove(this.keepKeys));
            }

            // добавляем follow элементы
            Result<UpWheres<InnerJoin>> reduceFollowUpWheres = new Result<>();
            Comparator<PushElement> comparator = getComparator(keepKeys);
            addJoins(element.getJoinFollows(reduceFollowUpWheres).it(), reduceFollowUpWheres.result,
                    comparator, newPriority, removedElements, mNewElements, mNewJoins, removedKeys);

            if(element instanceof PushGroup) {
                reducedPrim.set(true);
            } else {
                int i = 1, size = priority.size(); // докидываем не keep проверяя уменьшили мы ключ или нет
                while (!removedKeys.isEmpty() && i < size) {
                    SetFact.removeJavaAll(removedKeys, priority.get(i).getKeys());
                    i++;
                }
                reducedPrim.set(!removedKeys.isEmpty()); // уменьшили количество ключей (ессно не keep) или группировку
            }
            return new PushIteration<>(mNewElements.immutable(), mNewJoins.immutable(), newInnerOuter, keepKeys, newPriority);
        }
    }

    public static <WJ extends WhereJoin> void addJoins(Iterable<WJ> joins, UpWheres<WJ> upWheres, Comparator<PushElement> comparator, List<PushElement> newPriority, ImSet<? extends PushElement> elements, MExclSet<PushElement> mNewElements, MExclSet<WhereJoin> mNewJoins, Set<ParamExpr> removedKeys) {
        for(WJ joinFollow : joins) { // пытаемся заменить reduceJoin, на его joinFollows
            boolean found = false;
            for(PushElement newElement : elements)
                if(newElement.containsAll(joinFollow)) {
                    found = true;
                    break;
                }
            PushJoin followJoin = new PushJoin(joinFollow, upWheres.get(joinFollow));
            if(!found) {
                BaseUtils.addToOrderedList(newPriority, followJoin, 0, comparator);
                mNewJoins.exclAdd(joinFollow);
                mNewElements.exclAdd(followJoin);
            }

            if(removedKeys != null)
                SetFact.removeJavaAll(removedKeys, followJoin.getKeys());
        }
    }

    private <K extends Expr> void recPushJoins(PushIteration<K> iteration, KeyStat keyStat, StatType type, Stat innerRows, StatKeys<K> innerKeys, PushIteration.Reduce forceReduce, Result<BestResult> best, boolean upKeep) {

        if(!upKeep) // если сверху не обработали эту итерацию (здесь, а не в вырезании чтобы включить первую итерацию)
            forceReduce = iteration.checkBest(forceReduce, best, innerRows, innerKeys, keyStat, type);

        if(!iteration.hasElement())
            return;

        // оптимизация
        if(forceReduce == PushIteration.Reduce.PRIM && iteration.hasNoReducePrim())
            return;

        // проверяем оставление
        recPushJoins(iteration.keepElement(), keyStat, type, innerRows, innerKeys, forceReduce, best, true);

        // проверяем удаление
        Result<Boolean> reducedPrim = new Result<>();
        final PushIteration<K> removeIteration = iteration.removeElement(reducedPrim);

        if(removeIteration.getInnerOuter().isEmpty()) // если группировок не осталось выходим
            return;

        if (reducedPrim.result) // сбрасываем prim, если "ушел" один из значимых признаков (группировка или не keep ключ)
            forceReduce = PushIteration.Reduce.NONE;

        recPushJoins(removeIteration, keyStat, type, innerRows, innerKeys, forceReduce, best, false);
    }

    // может как MeanUpWheres сделать
    public static <J extends WhereJoin> UpWheres<J> andUpWheres(J[] wheres, UpWheres<J> up1, UpWheres<J> up2) {
        MExclMap<J, UpWhere> result = MapFact.mExclMap(wheres.length); // массивы
        for(J where : wheres) {
            UpWhere where1 = up1.get(where);
            UpWhere where2 = up2.get(where);
            UpWhere andWhere;
            if(where1==null)
                andWhere = where2;
            else
                if(where2==null)
                    andWhere = where1;
                else
                    andWhere = where1.and(where2);
            result.exclAdd(where, andWhere);
        }
        return new UpWheres<>(result.immutable());
    }

    public UpWheres<WhereJoin> andUpWheres(UpWheres<WhereJoin> up1, UpWheres<WhereJoin> up2) {
        return andUpWheres(wheres, up1, up2);
    }

    public UpWheres<WhereJoin> orUpWheres(UpWheres<WhereJoin> up1, UpWheres<WhereJoin> up2) {
        MExclMap<WhereJoin, UpWhere> result = MapFact.mExclMap(wheres.length); // массивы
        for(WhereJoin where : wheres)
            result.exclAdd(where, up1.get(where).or(up2.get(where)));
        return new UpWheres<>(result.immutable());
    }

    // из upMeans следует
    public UpWheres<WhereJoin> orMeanUpWheres(UpWheres<WhereJoin> up, WhereJoins meanWheres, UpWheres<WhereJoin> upMeans) {
        MExclMap<WhereJoin, UpWhere> result = MapFact.mExclMap(wheres.length); // массивы
        for(WhereJoin where : wheres) {
            UpWhere up2Where = upMeans.get(where);
            if(up2Where==null) { // то есть значит в следствии
                InnerExpr followExpr;
                for(WhereJoin up2Join : meanWheres.wheres)
                    if((followExpr=((InnerJoin)where).getInnerExpr(up2Join))!=null) {
                        up2Where = followExpr.getUpNotNullWhere();
                        break;
                    }
            }
            result.exclAdd(where, up.get(where).or(up2Where));
        }
        return new UpWheres<>(result.immutable());
    }
    
    // вообще при таком подходе, скажем из-за формул в ExprJoin, LEFT JOIN'ы могут быть раньше INNER, но так как SQL Server это позволяет бороться до конца за это не имеет особого смысла 
    public Where fillInnerJoins(UpWheres<WhereJoin> upWheres, MList<String> whereSelect, Result<Cost> mBaseCost, CompileSource source, ImSet<KeyExpr> keys, KeyStat keyStat) {
        Where innerWhere = Where.TRUE;
        for (WhereJoin where : wheres)
            if(!(where instanceof ExprIndexedJoin && ((ExprIndexedJoin)where).givesNoKeys())) {
                Where upWhere = upWheres.get(where).getWhere();
                String upSource = upWhere.getSource(source);
                if(where instanceof ExprJoin && ((ExprJoin)where).isClassJoin()) {
                    whereSelect.add(upSource);
                    innerWhere = innerWhere.and(upWhere);
                }
            }

        StatType statType = StatType.COMPILE;
        Result<ImSet<BaseExpr>> usedNotNulls = source.syntax.hasNotNullIndexProblem() ? new Result<ImSet<BaseExpr>>() : null;
        StatKeys<KeyExpr> statKeys = getStatKeys(keys, null, keyStat, statType, usedNotNulls);// newNotNull

        Cost baseCost = statKeys.getCost();
        if(mBaseCost.result != null)
            baseCost = baseCost.or(mBaseCost.result);
        mBaseCost.set(baseCost);

        if(usedNotNulls != null)
            for(BaseExpr notNull : usedNotNulls.result)
                whereSelect.add(notNull.getSource(source) + " IS NOT NULL");
        return innerWhere;
    }

    public ImSet<ParamExpr> getOuterKeys() {
        return AbstractOuterContext.getOuterKeys(this);
    }

    public ImSet<Value> getOuterValues() {
        return AbstractOuterContext.getOuterValues(this);
    }

    public boolean enumerate(ExprEnumerator enumerator) {
        return AbstractOuterContext.enumerate(this, enumerator);
    }

    public long getComplexity(boolean outer) {
        return AbstractOuterContext.getComplexity((OuterContext)this, outer);
    }

    public WhereJoins pack() {
        throw new RuntimeException("not supported yet");
    }

    public ImSet<StaticValueExpr> getOuterStaticValues() {
        throw new RuntimeException("should not be");
    }




    // EXACT OLD MECHANISM

    private static class OldEdge<K> {
        public BaseJoin<K> join;
        public Stat keyStat;
        public BaseExpr expr;

        public Stat getKeyStat(MAddMap<BaseJoin, Stat> statJoins) {
            return keyStat.min(statJoins.get(join));
        }
        public Stat getPropStat(MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, Stat> propStats) {
            return WhereJoins.getPropStat(expr, joinStats, propStats);
        }

        private OldEdge(BaseJoin<K> join, Stat keyStat, BaseExpr expr) {
            this.join = join;
            this.keyStat = keyStat;
            this.expr = expr;
        }

        public boolean equals(Object o) {
            return this == o || (o instanceof OldEdge && join.equals(((OldEdge<?>) o).join) && keyStat.equals(((OldEdge<?>) o).keyStat) && expr.equals(((OldEdge<?>) o).expr));
        }

        public int hashCode() {
            return 31 * (31 * join.hashCode() + keyStat.hashCode()) + expr.hashCode();
        }

        public String toString() {
            return join + ", " + keyStat + ", " + expr;
        }
    }

    private Iterable<OldEdge> getEdgePath(final Map<BaseJoin, ImMap<BaseJoin, OldEdge>> currentTree, final OldEdge startEdge, final BaseJoin endNode) {
        return new Iterable<OldEdge>() {
            public Iterator<OldEdge> iterator() {
                return new Iterator<OldEdge>() {
                    OldEdge currentEdge = startEdge;

                    public boolean hasNext() {
                        return currentEdge != null;
                    }

                    public OldEdge next() {
                        OldEdge nextEdge = currentEdge;
                        currentEdge = currentTree.get(currentEdge.join).get(endNode);
                        assert currentEdge != null || BaseUtils.hashEquals(nextEdge.join, endNode);
                        return nextEdge;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    private <K extends BaseExpr> StatKeys<K> getExactOldStatKeys(ImSet<K> groups, Result<Stat> rows, KeyStat keyStat, StatType type) {
        return getStatKeys(groups, rows, keyStat, null, null, null);
    }

    private MAddExclMap<BaseJoin, ImMap<BaseJoin, OldEdge>> removeEdge(Map<BaseJoin, ImMap<BaseJoin, OldEdge>> currentTree, OldEdge removeEdge) {
        BaseJoin to = removeEdge.join;
        MAddExclMap<BaseJoin, ImMap<BaseJoin, OldEdge>> rest = MapFact.mAddExclMap();
        final BaseJoin from = removeEdge.expr.getBaseJoin();
        final ImSet<BaseJoin> toNodes = currentTree.get(to).keys().addExcl(to);
        for(Map.Entry<BaseJoin, ImMap<BaseJoin, OldEdge>> entry : currentTree.entrySet()) {
            BaseJoin node = entry.getKey();
            ImMap<BaseJoin, OldEdge> nodes = entry.getValue();
            if(nodes.containsKey(from) || BaseUtils.hashEquals(node, from)) {
                rest.exclAdd(entry.getKey(), nodes);
                entry.setValue(nodes.removeIncl(toNodes));
            }
        }
        return rest;
    }

    // proceeded - из какой вершины в какую можно пройти и вершина через которую надо идти
    private Pair<Integer, ImSet<OldEdge>> recBuildMT(ImOrderSet<BaseJoin> order, ImMap<BaseJoin, ImSet<OldEdge>> edgesOuts, final MAddExclMap<BaseExpr, Stat> balancedStats, Set<OldEdge> removedEdges, int removedStat, Map<BaseJoin, ImMap<BaseJoin, OldEdge>> currentTree, int currentIndex, Pair<Integer, ImSet<OldEdge>> currentMin) {
        if(currentIndex >= order.size()) {
            return new Pair<>(removedStat, SetFact.fromJavaSet(removedEdges));
        }

        BaseJoin currentNode = order.get(currentIndex);
        ImSet<OldEdge> edgesOut = edgesOuts.get(currentNode);

        MExclMap<BaseJoin, OldEdge> edgeOutTree = MapFact.mExclMap();
        for(OldEdge edgeOut : edgesOut) {
            if(removedEdges.contains(edgeOut)) // избыточная проверка с точки зрения того что removedEdges содержит уже отработанные node'ы
                continue;
            ImMap<BaseJoin, OldEdge> reachableEdges = currentTree.get(edgeOut.join).addExcl(edgeOut.join, edgeOut);
            // нашли "два пути", edge на одном из путей надо вырезать рекурсивно выбираем минимум
            for(int i=0,size=reachableEdges.size();i<size;i++) {
                BaseJoin reachableJoin = reachableEdges.getKey(i);
//                OldEdge reachableEdge = reachableEdges.getValue(i);

                OldEdge presentEdgeOut = edgeOutTree.get(reachableJoin);
                if(presentEdgeOut != null) { // нашли цикл, один через edgeOut, второй через presentEdgeOut, один из edge'й на этих путях придется удалить в любом случае (это и перебираем)
                    // бежим по обоим найденным путям, упорядочив по минимальным весам
                    Iterable<OldEdge> edges = BaseUtils.sort(BaseUtils.mergeIterables(getEdgePath(currentTree, edgeOut, reachableJoin), getEdgePath(currentTree, presentEdgeOut, reachableJoin)), new Comparator<OldEdge>() {
                        public int compare(OldEdge o1, OldEdge o2) {
                            return Integer.compare(balancedStats.get(o1.expr).getWeight(), balancedStats.get(o2.expr).getWeight());
                        }});
                    for(OldEdge currentEdge : edges) {
                        // пробуем удалить ребро
                        int newRemovedStat = removedStat + balancedStats.get(currentEdge.expr).getWeight();
                        if(currentMin == null || currentMin.first > newRemovedStat) {
                            MAddExclMap<BaseJoin, ImMap<BaseJoin, OldEdge>> stackRemoved = removeEdge(currentTree, currentEdge);
                            removedEdges.add(currentEdge);
                            Pair<Integer, ImSet<OldEdge>> recCut = recBuildMT(order, edgesOuts, balancedStats, removedEdges, newRemovedStat, currentTree, currentIndex, currentMin);// придется начинать с 0 чтобы перестроить дерево
                            removedEdges.remove(currentEdge);
                            MapFact.addJavaAll(currentTree, stackRemoved);

                            if(recCut != null) {
                                assert currentMin == null || recCut.first < currentMin.first || recCut == currentMin;
                                currentMin = recCut;
                            }
                        }
                    }
                    return currentMin;
                } else
                    edgeOutTree.exclAdd(reachableJoin, edgeOut);
            }
        }
        currentTree.put(currentNode, edgeOutTree.immutable()); // assert что не было
        Pair<Integer, ImSet<OldEdge>> result = recBuildMT(order, edgesOuts, balancedStats, removedEdges, removedStat, currentTree, currentIndex + 1, currentMin);
        currentTree.remove(currentNode); // assert что было
        return result;
    }

    private Stat getMTCost(MAddMap<BaseJoin, Stat> joinStats, MAddExclMap<BaseExpr, Set<OldEdge>> balancedEdges, MAddExclMap<BaseExpr, Stat> balancedStats, Stat totalBalanced) {
        MExclMap<BaseJoin, MExclSet<OldEdge>> mEdges = MapFact.mExclMap();
        for(int i=0,size=joinStats.size();i<size;i++) {
            mEdges.exclAdd(joinStats.getKey(i), SetFact.<OldEdge>mExclSet());
        }
        for(int i=0;i<balancedEdges.size();i++) {
            BaseExpr bExpr = balancedEdges.getKey(i);
            BaseJoin from = bExpr.getBaseJoin();
            MExclSet<OldEdge> mFromEdges = mEdges.get(from);
            for(OldEdge edge : balancedEdges.getValue(i)) {
                assert BaseUtils.hashEquals(edge.expr, bExpr);
                mFromEdges.exclAdd(edge);
            }
        }
        ImMap<BaseJoin, ImSet<OldEdge>> edges = MapFact.immutable(mEdges);
        Pair<Integer, ImSet<OldEdge>> mt = recBuildMT(MapFact.buildGraphOrder(edges, new GetValue<BaseJoin, OldEdge>() {
            public BaseJoin getMapValue(OldEdge value) {
                return value.join;
            }}), edges, balancedStats, new HashSet<OldEdge>(), 0, new HashMap<BaseJoin, ImMap<BaseJoin, OldEdge>>(), 0, null);
        if(mt == null)
            return totalBalanced;
        return totalBalanced.div(new Stat(mt.first, true));
    }

    private Stat getEdgeRowStat(MAddMap<BaseJoin, Stat> joinStats, MAddExclMap<BaseExpr, Set<OldEdge>> balancedEdges, MAddExclMap<BaseExpr, Stat> balancedStats) {
        // высчитываем total

        int pessStatType = Settings.get().getPessStatType();

        Stat total = null;

        if(pessStatType != 3) {
            total = Stat.ONE;
            for (int i = 0; i < balancedEdges.size(); i++)
                total = total.mult(balancedStats.get(balancedEdges.getKey(i)).deg(balancedEdges.getValue(i).size()));
        }

        if(pessStatType == 0)
            return total;

        Stat mt = null;
        // multi tree stat
        if(pessStatType != 3) {
            mt = getMTCost(joinStats, balancedEdges, balancedStats, total);
            assert mt.lessEquals(total);

            if (pessStatType == 1)
                return mt;
        }

        // minimum spanning tree cost
        Stat mst = getMSTExCost(joinStats, balancedEdges, balancedStats);
        if(pessStatType == 3)
            return mst;

        assert mst.lessEquals(mt) && pessStatType == 2;
        return mst.avg(mt);
    }

    private Stat getMSTExCost(MAddMap<BaseJoin, Stat> joinStats, MAddExclMap<BaseExpr, Set<OldEdge>> balancedEdges, MAddExclMap<BaseExpr, Stat> balancedStats) {
        int nodes = 0; int edges = 0;

        SpanningTreeWithBlackjack<BaseJoin> graph = new SpanningTreeWithBlackjack<>();
        for(int i=0,size=joinStats.size();i<size;i++) {
            BaseJoin node = joinStats.getKey(i);
            graph.addNode(node, node.getJoins().isEmpty() ? 0 : joinStats.getValue(i).getWeight());
            nodes++;
        }

        for(int i=0;i<balancedEdges.size();i++) {
            BaseExpr bExpr = balancedEdges.getKey(i);
            BaseJoin from = bExpr.getBaseJoin();
            for(OldEdge edge : balancedEdges.getValue(i)) {
                assert BaseUtils.hashEquals(edge.expr, bExpr);
                graph.addEdge(from, edge.join, balancedStats.get(bExpr).getWeight());
                edges++;
            }
        }

        int maxIterations = Settings.get().getMaxEdgeIterations();
        return new Stat(graph.calculate(BaseUtils.max(edges - nodes, 1) * maxIterations), true);
    }

    // assert что rows >= result
    // можно rows в StatKeys было закинуть как и ExecCost, но используется только в одном месте и могут быть проблемы с кэшированием
    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, Result<Stat> rows, final KeyStat keyStat, Result<BaseExpr> newNotNull, MAddMap<BaseExpr, Boolean> proceededNotNulls, Result<Cost> tableCosts) {

        // groups учавствует только в дополнительном фильтре
        final MAddMap<BaseJoin, Stat> joinStats = MapFact.mAddOverrideMap();
        final MAddMap<BaseExpr, Stat> exprStats = MapFact.mAddOverrideMap();

        final MAddMap<Table.Join, Stat> indexedStats = MapFact.<Table.Join, Stat>mAddOverrideMap();

        MAddExclMap<BaseExpr, Set<OldEdge>> balancedEdges = MapFact.mAddExclMap(); // assert edge.expr == key
        MAddExclMap<BaseExpr, Stat> balancedStats = MapFact.mAddExclMap();

        buildBalancedGraph(groups, keyStat, joinStats, exprStats, balancedEdges, balancedStats, newNotNull, proceededNotNulls, indexedStats);

        // pessimistic adjust - строим остовное дерево (на
        Stat edgeRowStat = getEdgeRowStat(joinStats, balancedEdges, balancedStats);

        // бежим по всем сбалансированным ребрам суммируем, бежим по всем нодам суммируем, возвращаем разность
        Stat rowStat = Stat.ONE;
        for(int i=0,size=joinStats.size();i<size;i++)
            rowStat = rowStat.mult(joinStats.getValue(i));
        final Stat finalStat = rowStat.div(edgeRowStat);

        if(rows!=null)
            rows.set(finalStat);

        Stat tableStat = Stat.ONE;
        for(int i=0,size=indexedStats.size();i<size;i++)
            tableStat = tableStat.or(indexedStats.getValue(i));
        if(tableCosts != null) {
            tableCosts.set(new Cost(tableStat));
        }

        DistinctKeys<K> distinct = new DistinctKeys<>(groups.mapValues(new GetValue<Stat, K>() {
            public Stat getMapValue(K value) { // для groups, берем min(из статистики значения, статистики его join'а)
                return getPropStat(value, joinStats, exprStats).min(finalStat);
            }
        }));
        return StatKeys.create(new Cost(tableStat.max(finalStat)), finalStat, distinct); // возвращаем min(суммы groups, расчитанного результата)
    }

    private <K extends BaseExpr> void buildBalancedGraph(ImSet<K> groups, KeyStat keyStat, MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, Stat> exprStats, MAddExclMap<BaseExpr, Set<OldEdge>> balancedEdges, MAddExclMap<BaseExpr, Stat> balancedStats, Result<BaseExpr> newNotNull, MAddMap<BaseExpr, Boolean> proceededNotNulls, MAddMap<Table.Join, Stat> indexedStats) {
        Set<OldEdge> edges = SetFact.mAddRemoveSet();

        buildGraph(groups, keyStat, exprStats, joinStats, edges, proceededNotNulls, newNotNull);

        balanceGraph(joinStats, exprStats, edges, balancedEdges, balancedStats, indexedStats);
    }

    // balancedEdges - исходящие edges для всех "внутренних" expr, название конечно не совсем корректное
    // balancedStats - уже скорректированная статистика, только для "внутренних" expr, не включая groups, в принципе можно совместить с exprStats, пробежав по groups и хакинув туда getPropStat(value, joinStats, exprStats), но пока особого смысла нет
    private void balanceGraph(MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, Stat> exprStats, Set<OldEdge> unbalancedEdges, MAddExclMap<BaseExpr, Set<OldEdge>> balancedEdges, MAddExclMap<BaseExpr, Stat> balancedStats, MAddMap<Table.Join, Stat> indexedStats) {
        // ищем несбалансированное ребро с минимальной статистикой
        Stat currentStat = null;
        MAddExclMap<BaseExpr, Set<OldEdge>> currentBalancedEdges = MapFact.mAddExclMap();

        if(indexedStats != null)
            for(int i=0,size=joinStats.size();i<size;i++) {
                BaseJoin join = joinStats.getKey(i);
                if(join instanceof Table.Join)
                    indexedStats.add((Table.Join)join, joinStats.getValue(i));
            }

        while(unbalancedEdges.size() > 0 || currentBalancedEdges.size() > 0) {
            OldEdge<?> unbalancedEdge = null;
            Pair<Stat, Stat> unbalancedStat = null;

            Stat stat = Stat.MAX;
            for(OldEdge edge : unbalancedEdges) {
                Stat keys = edge.getKeyStat(joinStats);
                Stat values = edge.getPropStat(joinStats, exprStats);
                Stat min = keys.min(values);
                if(min.less(stat)) { // если нашли новый минимум про старый забываем
                    unbalancedEdge = edge;
                    unbalancedStat = new Pair<>(keys, values);
                    stat = min;
                    if(currentStat !=null && stat.equals(currentStat)) // оптимизация, так как меньше уже быть не может
                        break;
                }
            }
            if(currentStat==null || !stat.equals(currentStat)) { // закончилась группа статистики
                if(currentStat!=null) { // не первая
                    assert currentStat.less(stat);
                    for(int i=0;i<currentBalancedEdges.size();i++) { // переливаем в balanced
                        BaseExpr expr = currentBalancedEdges.getKey(i);
                        Set<OldEdge> exprEdges = currentBalancedEdges.getValue(i);

                        for(int j=0;j<balancedEdges.size();j++) { // бежим по всем уже сбалансированным и пытаемся поддержать cross-column статистику
                            BaseExpr bExpr = balancedEdges.getKey(j);
                            Set<OldEdge> bExprEdges = balancedEdges.getValue(j);
                            Stat bStat = balancedStats.get(bExpr);

                            List<Pair<OldEdge, OldEdge>> mergeEdges = new ArrayList<>();
                            Iterator<OldEdge> it = exprEdges.iterator();
                            while(it.hasNext()) {
                                OldEdge exprEdge = it.next();

                                OldEdge bExprEdge = null;
                                boolean found = false;
                                Iterator<OldEdge> bit = bExprEdges.iterator();
                                while(bit.hasNext()) {
                                    bExprEdge = bit.next();
                                    if(BaseUtils.hashEquals(exprEdge.join, bExprEdge.join)) {
                                        found = true;
                                        bit.remove();
                                        break;
                                    }
                                }
                                if(found) {
                                    it.remove();
                                    mergeEdges.add(new Pair<>(exprEdge, bExprEdge));
                                }
                            }

                            if(mergeEdges.size() > 1) { // если пара используется несколько раз объединим
                                ConcatenateExpr concExpr = new ConcatenateExpr(ListFact.toList(expr, bExpr)); // создаем общую вершину
                                InnerBaseJoin<?> concJoin = concExpr.getBaseJoin();
                                Stat mergedStat = currentStat.mult(bStat);
//                                balanced = balanced.mult(mergedStat); // добавляем два внутренних edge'а (обработанных), собсно так как они потом не будут использовать просто добавим в статистику
                                exprEdges.add(new OldEdge(concJoin, currentStat, expr));
                                bExprEdges.add(new OldEdge(concJoin, bStat, bExpr));
                                joinStats.add(concJoin, mergedStat); exprStats.add(concExpr, mergedStat); // добавляем join \ записываем статистику
                                for(Pair<OldEdge, OldEdge> mergeEdge : mergeEdges) { // добавляем внешние (возможно не сбалансированные edge'и)
                                    assert BaseUtils.hashEquals(mergeEdge.first.join, mergeEdge.second.join);
                                    OldEdge mergedEdge = new OldEdge(mergeEdge.first.join, mergedStat, concExpr);
                                    unbalancedEdges.add(mergedEdge);
                                }
                                unbalancedEdge = null; // сбрасываем текущую итерацию и начинаем заново
                            } else {
                                if(mergeEdges.size()==1) { // вернем на место
                                    Pair<OldEdge, OldEdge> single = BaseUtils.single(mergeEdges);
                                    exprEdges.add(single.first); bExprEdges.add(single.second);
                                }
                            }
                        }

                        balancedEdges.exclAdd(expr, exprEdges); // закидываем в balanced
                        balancedStats.exclAdd(expr, currentStat);
                    }
                }

                if(unbalancedEdge!=null)
                    currentStat = stat;
                currentBalancedEdges = MapFact.mAddExclMap();
            }

            if(unbalancedEdge!=null) { // потому что edges может быть пустой или объединенные ребра будут с минимальной статистикой
                BaseJoin decreaseJoin; Stat decrease; boolean keyReduce = false;
                if(unbalancedStat.first.less(unbalancedStat.second)) { // балансируем значение
                    decrease = unbalancedStat.second.div(unbalancedStat.first);
                    exprStats.add(unbalancedEdge.expr, unbalancedStat.first); // это и есть разница
                    decreaseJoin = unbalancedEdge.expr.getBaseJoin();
                } else { // балансируем ключ, больше он использоваться не будет
                    decrease = unbalancedStat.first.div(unbalancedStat.second);
                    decreaseJoin = unbalancedEdge.join;
                    keyReduce = true;
                }
                joinStats.add(decreaseJoin, joinStats.get(decreaseJoin).div(decrease));
                unbalancedEdges.remove(unbalancedEdge);

                // помечаем уменьшения статистики по индексу приполагается что будет bitmap scan с bitmap and всех индексов
                if(indexedStats != null && decreaseJoin instanceof Table.Join && (keyReduce || (unbalancedEdge.expr.isIndexed() && !(unbalancedEdge.join instanceof CalculateJoin))))
                    indexedStats.add((Table.Join) decreaseJoin, indexedStats.get((Table.Join) decreaseJoin).div(decrease));

                Set<OldEdge> exprEdges = currentBalancedEdges.get(unbalancedEdge.expr);
                if(exprEdges==null) {
                    exprEdges = SetFact.mAddRemoveSet();
                    currentBalancedEdges.exclAdd(unbalancedEdge.expr, exprEdges);
                }
                exprEdges.add(unbalancedEdge);
            }
        }
    }

    private <K extends BaseExpr> void buildGraph(ImSet<K> groups, KeyStat keyStat, MAddMap<BaseExpr, Stat> exprStats, MAddMap<BaseJoin, Stat> joinStats, Set<OldEdge> edges, MAddMap<BaseExpr, Boolean> proceededNotNulls, Result<BaseExpr> newNotNull) {
        Set<BaseExpr> exprs = SetFact.mAddRemoveSet();
        Set<BaseJoin> joins = SetFact.mAddRemoveSet();

        buildEdgesExprsJoins(groups, keyStat, edges, exprs, joins);

        for(BaseJoin join : joins)
            joinStats.add(join, join.getStatKeys(keyStat, StatType.ALL, true).getRows());

        int intStat = Settings.get().getAverageIntervalStat();
        if(intStat >= 0)
            for(ExprIndexedJoin join : ExprIndexedJoin.getIntervals(wheres))
                joinStats.add(join, new Stat(intStat, true));

        // читаем статистику по значениям
        for(BaseExpr expr : exprs) {
            PropStat exprStat = expr.getStatValue(keyStat, StatType.ALL);
            exprStats.add(expr, exprStat.distinct);

            Stat notNullStat = exprStat.notNull;
            Boolean proceededNotNull = null;
            if(notNullStat !=null && !(newNotNull != null && (proceededNotNull = proceededNotNulls.get(expr)) != null && proceededNotNull)) { // пропускаем notNull
                InnerBaseJoin<?> notNullJoin = expr.getBaseJoin();
                Stat joinStat = joinStats.get(notNullJoin);
//                assert notNullStat.lessEquals(joinStat);
                joinStats.add(notNullJoin, notNullStat.min(joinStat)); // уменьшаем статистику join'а до notNull значения, min нужен так как может быть несколько notNull
                if(newNotNull != null && proceededNotNull == null && notNullStat.less(joinStat) && expr.isIndexed()) { // если уменьшаем статистику, индексированы, и есть проблема с notNull
                    newNotNull.set(expr);
                }
            }
        }
    }

    private <K extends BaseExpr> void buildEdgesExprsJoins(ImSet<K> groups, KeyStat keyStat, Set<OldEdge> edges, Set<BaseExpr> exprs, Set<BaseJoin> joins) {
        // собираем все ребра и вершины
        Queue<BaseJoin> queue = new LinkedList<>();
        for(WhereJoin valueJoin : wheres) {
            queue.add(valueJoin);
            joins.add(valueJoin);
        }
        for(BaseExpr group : groups) {
            exprs.add(group);
            InnerBaseJoin<?> valueJoin = group.getBaseJoin();
            if(!joins.contains(valueJoin)) {
                queue.add(valueJoin);
                joins.add(valueJoin);
            }
        }
        while(!queue.isEmpty()) {
            BaseJoin<Object> join = queue.poll();
            ImMap<?, BaseExpr> joinExprs = getJoinsForStat(join);

/*            if(((BaseJoin)join) instanceof UnionJoin) { // UnionJoin может потерять ключи, а они важны
                for(ParamExpr lostKey : ((UnionJoin) (BaseJoin)join).getLostKeys())
                    if(!joins.contains(lostKey)) {
                        queue.add(lostKey);
                        joins.add(lostKey);
                    }
            }*/

            for(int i=0,size=joinExprs.size();i<size;i++) {
                Object joinKey = joinExprs.getKey(i);
                BaseExpr joinExpr = joinExprs.getValue(i);

                edges.add(new OldEdge(join, join.getStatKeys(keyStat, StatType.ALL, true).getDistinct(joinKey), joinExpr));

                exprs.add(joinExpr);
                InnerBaseJoin<?> valueJoin = joinExpr.getBaseJoin();
                if(!joins.contains(valueJoin)) {
                    queue.add(valueJoin);
                    joins.add(valueJoin);
                }
            }
        }
    }




}
