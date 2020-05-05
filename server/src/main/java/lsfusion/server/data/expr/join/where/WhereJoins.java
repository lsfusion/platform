package lsfusion.server.data.expr.join.where;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddCol;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSSVSMap;
import lsfusion.base.col.lru.LRUWVSMap;
import lsfusion.base.dnf.ExtraMultiIntersectSetWhere;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.base.log.DebugInfoWriter;
import lsfusion.base.tree.GreedyTreeBuilding;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.ContextEnumerator;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.inner.InnerExpr;
import lsfusion.server.data.expr.join.base.BaseJoin;
import lsfusion.server.data.expr.join.base.UnionJoin;
import lsfusion.server.data.expr.join.base.ValueJoin;
import lsfusion.server.data.expr.join.inner.InnerBaseJoin;
import lsfusion.server.data.expr.join.inner.InnerJoin;
import lsfusion.server.data.expr.join.inner.InnerJoins;
import lsfusion.server.data.expr.join.query.GroupJoin;
import lsfusion.server.data.expr.join.query.LastJoin;
import lsfusion.server.data.expr.join.query.PartitionJoin;
import lsfusion.server.data.expr.join.query.QueryJoin;
import lsfusion.server.data.expr.join.select.ExprIndexedJoin;
import lsfusion.server.data.expr.join.select.ExprIntervalJoin;
import lsfusion.server.data.expr.join.select.ExprJoin;
import lsfusion.server.data.expr.join.select.ExprStatJoin;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.value.StaticValueExpr;
import lsfusion.server.data.query.LimitOptions;
import lsfusion.server.data.query.compile.where.UpWhere;
import lsfusion.server.data.query.compile.where.UpWheres;
import lsfusion.server.data.stat.*;
import lsfusion.server.data.translate.JoinExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.DNFWheres;
import lsfusion.server.data.where.Where;
import lsfusion.server.physics.admin.Settings;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

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
            return BaseUtils.immutableCast(element.getJoinFollows(new Result<>(), null).it());
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
            for(WhereJoin where : getAdjWheres())
                calcInnerJoins = calcInnerJoins.and(where.getInnerJoins());
            innerJoins = calcInnerJoins;
        }
        return innerJoins;
    }    
    public static ImOrderSet<InnerJoin> getInnerJoinOrder(ImOrderSet<BaseJoin> whereJoinOrder) {
        MOrderSet<InnerJoin> mInnerJoinOrder = SetFact.mOrderSet(); // не excl из-за valueJoins
        for(BaseJoin join : whereJoinOrder)
            if(join instanceof WhereJoin)
                ((WhereJoin)join).getInnerJoins().fillInnerJoinOrder(mInnerJoinOrder); // чтобы учесть valueJoins
        return mInnerJoinOrder.immutableOrder();
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
        return SetFact.toExclSet(wheres);
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
        return getStatKeys(groups, rows, keyStat, type, null, null);
    }

    // assert что rows >= result
    // можно rows в StatKeys было закинуть как и Cost, но используется только в одном месте и могут быть проблемы с кэшированием
    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, Result<Stat> rows, final KeyStat keyStat, StatType type, Result<CompileInfo> compileInfo, DebugInfoWriter debugInfoWriter) {
        return getCostStatKeys(groups, rows, keyStat, type, compileInfo, debugInfoWriter);
    }

    private static class JoinCostStat<Z> extends CostStat {
        private final BaseJoin<Z> join;
        private final StatKeys<Z> statKeys;

        public JoinCostStat(BaseJoin<Z> join, StatKeys<Z> statKeys, boolean compileInfo, BitSet inJoins, BitSet adjJoins, PushCost pushCost) {
            this(join, statKeys, compileInfo ? SetFact.EMPTY() : null, compileInfo, inJoins, adjJoins, pushCost);
        }
        public JoinCostStat(BaseJoin<Z> join, StatKeys<Z> statKeys, ImSet<BaseExpr> usedNotNulls, boolean compileInfo, BitSet inJoins, BitSet adjJoins, PushCost pushCost) {
            super(compileInfo ? new CompileInfo(usedNotNulls, SetFact.singletonOrder(join), join instanceof QueryJoin ? statKeys.getCost() : Cost.ONE) : null, inJoins, adjJoins, join instanceof QueryJoin ? MapFact.singleton((QueryJoin)join, pushCost) : null);
            this.join = join;
            this.statKeys = statKeys;
        }

        public BaseJoin<Z> getJoin() {
            return join;
        }

        @Override
        public Cost getCost() {
            return statKeys.getCost();
        }

        @Override
        public Cost getCostWithLookAhead() {
            return statKeys.getCost();
        }

        @Override
        public Stat getStat() {
            return statKeys.getRows();
        }

        @Override
        public Cost getMinCost() {
            return statKeys.getCost();
        }

        @Override
        public Cost getMaxCost() {
            return statKeys.getCost();
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
            return SetFact.singleton(join);
        }

        @Override
        public ImMap<BaseJoin, Stat> getJoinStats() {
            return MapFact.singleton(join, statKeys.getRows());
        }

        @Override
        public ImMap<BaseJoin, DistinctKeys> getKeyStats() {
            return MapFact.singleton(join, statKeys.getDistinct());
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
        public ImSet getPushKeys(QueryJoin pushJoin) {
            return null;
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
        
        private final Cost lookAheadCost;

        // доппараметры, в основном для детерменированности
        private final Cost leftCost; // assert что больше внутренних
        private final Cost rightCost; // assert что больше left и больше внутренних (то есть max) 
        private final Stat leftStat;
        private final Stat rightStat;
        private final int joinsCount;

        // путь
        private final ImMap<BaseJoin, Stat> joinStats;  // минимум по статистике с момента появления этого join'а в дереве;
        private final ImMap<BaseJoin, DistinctKeys> keyStats; // поддерживаем только потому что getPushedStatKeys может их "уточнять"
        private final ImMap<BaseExpr, Stat> propStats; // поддерживаем только потому что getPushedStatKeys может их "уточнять"

        // debug info, temporary
        private static class DebugInfo {
            private final CostStat left;
            private final CostStat right;
            private final Stat[] aEdgeStats;
            private final Stat[] bEdgeStats;
            private final ImList<? extends Edge> edges;

            public DebugInfo(CostStat left, CostStat right, Stat[] aEdgeStats, Stat[] bEdgeStats, ImList<? extends Edge> edges) {
                this.left = left;
                this.right = right;
                this.aEdgeStats = aEdgeStats;
                this.bEdgeStats = bEdgeStats;
                this.edges = edges;
            }

            public String toString(String prefix) {
                return " LEFT : "
                        + Arrays.toString(aEdgeStats) + " RIGHT : " + Arrays.toString(bEdgeStats) + "\n" + (left != null ? left.toString(prefix + '\t') : "") + '\n' + (right != null ? right.toString(prefix + '\t') : "");
            }
        }
        private final DebugInfo debugInfo;

        public MergeCostStat(Cost lookAheadCost, MergeCostStat costStat) {
            this(costStat.cost, lookAheadCost, costStat.stat, costStat.inJoins, costStat.adjJoins,
                    costStat.leftCost, costStat.rightCost, costStat.leftStat, costStat.rightStat, costStat.joinsCount,
                    costStat.joinStats, costStat.keyStats, costStat.propStats, costStat.pushCosts, costStat.compileInfo 
                    , costStat.debugInfo
            );
        }

        public MergeCostStat(Cost cost, Stat stat, BitSet inJoins, BitSet adjJoins,
                             Cost leftCost, Cost rightCost, Stat leftStat, Stat rightStat, int joinsCount
                             , DebugInfo debugInfo
                            ) {
            this(cost, null, stat, inJoins, adjJoins,
                    leftCost, rightCost, leftStat, rightStat, joinsCount,
                    null, null, null, null, null
                    , debugInfo
                );
        }

        public MergeCostStat(Cost cost, Cost lookAheadCost, Stat stat, BitSet inJoins, BitSet adjJoins,
                             Cost leftCost, Cost rightCost, Stat leftStat, Stat rightStat, int joinsCount,
                             ImMap<BaseJoin, Stat> joinStats, ImMap<BaseJoin, DistinctKeys> keyStats, ImMap<BaseExpr, Stat> propStats, ImMap<QueryJoin, PushCost> pushCosts, CompileInfo compileInfo
                             , DebugInfo debugInfo
                            ) {
            super(compileInfo, inJoins, adjJoins, pushCosts);
            this.cost = cost;
            this.lookAheadCost = lookAheadCost;
            this.stat = stat;

            this.leftCost = leftCost;
            this.rightCost = rightCost;
            this.leftStat = leftStat;
            this.rightStat = rightStat;
            this.joinsCount = joinsCount;

            this.joinStats = joinStats;
            this.keyStats = keyStats;
            this.propStats = propStats; // assert что все expr.getBaseJoin() из joinStats

            this.debugInfo = debugInfo;
        }

        @Override
        public BaseJoin getJoin() {
            return null;
        }

        @Override
        public Cost getCost() {
            return cost;
        }

        @Override
        public Cost getCostWithLookAhead() {
            Cost result = cost;
            if(lookAheadCost != null)
                result = result.or(lookAheadCost);
            return result;
        }

        @Override
        public Stat getStat() {
            return stat;
        }

        @Override
        public Cost getMinCost() {
            return leftCost;
        }

        @Override
        public Cost getMaxCost() {
            return rightCost;
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
        public ImSet getPushKeys(QueryJoin pushJoin) {
            return pushCosts.get(pushJoin).pushKeys;
        }

        public String toString(String prefix) {
            return prefix + "m" + getCost() + " " + getStat() + (debugInfo != null ? debugInfo.toString(prefix) : "");
        }
    }
    
    private static class PushCost {
        private final StatKeys pushStatKeys; // важно получить хороший именно pushStatKeys (то есть проталкивание), а не финальную статистику
        
        private final Cost leftCost; // чтобы выбирались предикаты с меньшими cost'ами
        
        // может быть null если проталкивания нет
        private boolean noPush;
        private final ImSet pushKeys; // для getPushKeys + чтобы, с использованием leftCost не выбирать предикаты у которых вообще нет общих связей с проталкиваемым предикатом 

        public PushCost(StatKeys pushStatKeys, Cost leftCost, boolean noPush, ImSet pushKeys) {
            this.pushStatKeys = pushStatKeys;
            this.leftCost = leftCost;
            this.noPush = noPush;
            this.pushKeys = pushKeys;
            assert !noPush || pushKeys == null;
        }
        
        private boolean isCartesian() {
            return !noPush && (pushKeys == null || pushKeys.isEmpty());
        }

        private int pushCompareTo(PushCost b, boolean pushLargeDepth) {
            int compare = Integer.compare(pushStatKeys.getCost().rows.getWeight(), b.pushStatKeys.getCost().rows.getWeight());
            if(compare != 0)
                return compare;
            if(pushLargeDepth)
                return 0;
            compare = Integer.compare(pushStatKeys.getRows().getWeight(), b.pushStatKeys.getRows().getWeight());
            if(compare != 0)
                return compare;

            // если проталкиваемые предикаты имеют меньший cost, берем их (чисто пессимистичная оценка, то есть если статистика неоднородна, лучше протолкнуть более ограничивающий предикат)
            // при этом важно чтобы pushKeys был иначе смысла в leftCost не будет (значит там valueJoin и декартово)

            // декартово хуже - наличие pushKeys лучше чем отсутствие
            compare = Boolean.compare(isCartesian(), b.isCartesian());
            if(compare != 0)
                return compare;

            if(leftCost.less(Cost.ALOT) || b.leftCost.less(Cost.ALOT)) { // if cost is ALOT, no need to push, because it can be simple keyExpr, so TRUE will be pushed, which is not efficient + will break some assertions 
                compare = Integer.compare(leftCost.rows.getWeight(), b.leftCost.rows.getWeight());
                if (compare != 0) 
                    return compare;
            }

            return compare;
        }
    }
    
    public static class CompileInfo {
        protected final ImSet<BaseExpr> usedNotNulls;
        protected final ImOrderSet<BaseJoin> joinOrder;
        protected final Cost maxSubQueryCost;

        public CompileInfo(ImSet<BaseExpr> usedNotNulls, ImOrderSet<BaseJoin> joinOrder, Cost maxSubQueryCost) {
            this.usedNotNulls = usedNotNulls;
            this.joinOrder = joinOrder;
            this.maxSubQueryCost = maxSubQueryCost;
        }

        public CompileInfo add(CompileInfo compileInfo) {
            return new CompileInfo(usedNotNulls.addExcl(compileInfo.usedNotNulls), joinOrder.addOrderExcl(compileInfo.joinOrder), maxSubQueryCost.or(compileInfo.maxSubQueryCost));
        }
        
        public final static CompileInfo EMPTY = new CompileInfo(SetFact.EMPTY(), SetFact.EMPTYORDER(), Cost.ONE); 
    }

    private abstract static class CostStat implements Comparable<CostStat> {

        protected final BitSet inJoins; // повторяет getJoins чисто для оптимизации
        protected final BitSet adjJoins;

        // проталкивание
        protected final ImMap<QueryJoin, PushCost> pushCosts;

        public boolean adjacent(CostStat costStat) { // есть общее ребро
            return costStat.adjJoins.intersects(inJoins) || costStat.inJoins.intersects(adjJoins);
        }

        public boolean adjacentCommon(CostStat costStat) { // есть вершина, для которой есть общие ребра
            return costStat.adjJoins.intersects(adjJoins); 
        }
        
        public boolean adjacentWithCommon(CostStat costStat) {
            assert !inJoins.intersects(costStat.inJoins);
            return adjacent(costStat) || adjacentCommon(costStat);
        }                
                
        public CostStat(CompileInfo compileInfo, BitSet inJoins, BitSet adjJoins, ImMap<QueryJoin, PushCost> pushCosts) {
            this.compileInfo = compileInfo;
            this.inJoins = inJoins;
            this.adjJoins = adjJoins;
            this.pushCosts = pushCosts;
            assert (inJoins == null && adjJoins == null) || !inJoins.intersects(adjJoins);
        }

        public abstract BaseJoin getJoin();
        public abstract Cost getCost();
        public abstract Cost getCostWithLookAhead();
        public abstract Stat getStat();

        public abstract Cost getMinCost();
        public abstract Cost getMaxCost();
        public abstract Stat getMinStat();
        public abstract Stat getMaxStat();
        public abstract int getJoinsCount();
        
        public ImMap<QueryJoin, PushCost> getPushCosts() {
            return pushCosts;
        }

        public abstract ImSet<BaseJoin> getJoins();
        public abstract ImMap<BaseJoin, Stat> getJoinStats();
        public abstract ImMap<BaseJoin, DistinctKeys> getKeyStats();
        public abstract ImMap<BaseExpr, Stat> getPropStats();

        public abstract PropStat getPropStat(BaseExpr expr, MAddMap<BaseExpr, PropStat> exprStats);
        public abstract <K> Stat getKeyStat(BaseJoin<K> join, K key);

        // compile параметры
        protected final CompileInfo compileInfo;
        
        public abstract ImSet getPushKeys(QueryJoin pushJoin);

        private <K> Stat getKeyStat(Edge<K> edge) {
            return getKeyStat(edge.join, edge.key);
        }

        private <K extends BaseExpr> ImMap<K, Stat> getDistinct(ImSet<K> exprs, final MAddMap<BaseExpr, PropStat> exprStats) {
            return new DistinctKeys<>(exprs.mapValues(new Function<K, Stat>() {
                public Stat apply(K value) {
                    return getPropStat(value, exprStats).distinct;
                }
            }));
        }

        private static ImMap<QueryJoin, PushCost> addPushCosts(ImMap<QueryJoin, PushCost> left, ImMap<QueryJoin, PushCost> right) {
            if(left == null) // оптимизация
                return right;
            if(right == null)
                return left;
            return right.addExcl(left);
        }

        private static int pushCompareTo(ImMap<QueryJoin, PushCost> a, ImMap<QueryJoin, PushCost> b, QueryJoin pushJoin, boolean pushLargeDepth) {
            // сначала проверяем pushJoin 
            int compare;
            compare = a.get(pushJoin).pushCompareTo(b.get(pushJoin), pushLargeDepth);
            if(compare != 0 || pushLargeDepth)
                return compare;
            // looking for other pushes
            int aGreater = 0;
            int bGreater = 0;
            for(int i=0,size=a.size();i<size;i++) {
                QueryJoin key = a.getKey(i);
                if(BaseUtils.hashEquals(key, pushJoin))
                    continue;
                
                PushCost aCost = a.getValue(i);

                PushCost bCost = b.get(key);
                if(bCost != null) {
                    compare = aCost.pushCompareTo(bCost, false);
                    if(compare != 0) {
                        if(compare > 0)
                            aGreater++;
                        else
                            bGreater++;
                    }
                }
            }
            
            compare = Integer.compare(aGreater, bGreater);
            if(compare != 0)
                return compare;

            return Integer.compare(a.size(), b.size());
        }

        public int pushCompareTo(CostStat o, QueryJoin pushJoin, boolean pushLargeDepth) {
            MergeCostStat mStat = (MergeCostStat) o;
            return pushCompareTo(getPushCosts(), mStat.getPushCosts(), pushJoin, pushLargeDepth);
        }

        @Override
        public int compareTo(CostStat o) {
            if(this == max)
                return o == max ? 0 : 1;
            if(o == max)
                return -1;

            int compare = Integer.compare(getCostWithLookAhead().rows.getWeight(), o.getCostWithLookAhead().rows.getWeight());
            if(compare != 0)
                return compare;
            compare = Integer.compare(getStat().getWeight(), o.getStat().getWeight());
            if(compare != 0)
                return compare;
            compare = Integer.compare(getMaxCost().rows.getWeight(), o.getMaxCost().rows.getWeight());
            if(compare != 0) // у кого max cost больше лучше
                return -compare;
            compare = Integer.compare(getMinCost().rows.getWeight(), o.getMinCost().rows.getWeight());
            if(compare != 0) // у кого min cost больше лучше
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
        T calculate(CostStat costStat, MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, PropStat> exprStats);
    }

    public <K extends BaseExpr, T> T calculateCost(ImSet<K> groups, boolean compileInfo, final KeyStat keyStat, final StatType type, CostResult<T> result, QueryJoin pushJoin, Result<PushInfo> pushInfo, UpWheres<WhereJoin> pushUpWheres, boolean pushLargeDepth, DebugInfoWriter debugInfoWriter) {
        // вообще по хорошему надо сделать переборный жадняк, то есть выбрать ребра с минимальной суммой из costReduce + cost (то есть важно и то и то), и искуственно повышать приоритет соединения node'ов (чтобы они соединялись в самом конце), решит проблему 0-5-0-0-5-0

        final MAddMap<BaseJoin, Stat> joinStats = MapFact.mAddOverrideMap();
        final MAddMap<BaseJoin, DistinctKeys> keyDistinctStats = MapFact.mAddOverrideMap();
        final MAddMap<BaseExpr, PropStat> exprStats = MapFact.mAddOverrideMap();
        final MAddMap<BaseJoin, Cost> indexedStats = MapFact.mAddOverrideMap();
        Result<ImSet<Edge>> edges = new Result<>();
        
        buildGraphWithStats(groups, edges, joinStats, exprStats, null, keyDistinctStats, indexedStats, type, keyStat, pushJoin, pushInfo, pushUpWheres);

        CostStat costStat = getCost(pushJoin, pushLargeDepth, compileInfo, joinStats, indexedStats, exprStats, keyDistinctStats, edges.result, keyStat, type, debugInfoWriter);

        return result.calculate(costStat, joinStats, exprStats);
    }

    public <K extends BaseExpr, Z> StatKeys<K> getCostStatKeys(final ImSet<K> groups, final Result<Stat> rows, final KeyStat keyStat, final StatType type, final Result<CompileInfo> compileInfo, DebugInfoWriter debugInfoWriter) {
        // нужно отдельно STAT считать, так как при например 0 - 3, 0 - 100 получит 3 - 100 -> 3, а не 0 - 3 -> 3 и соотвественно статистику 3, а не 0
        //  но пока не принципиально будем брать stat из "плана"

        if(isFalse() && groups.isEmpty()) {
            if(rows != null)
                rows.set(Stat.ONE);
            if(compileInfo != null)
                compileInfo.set(CompileInfo.EMPTY);
            return new StatKeys<>(groups, Stat.ONE);
        }

        return calculateCost(groups, compileInfo != null, keyStat, type, (costStat, joinStats, exprStats) -> {
            Stat stat = costStat.getStat();
            Cost cost = costStat.getCost();
            if (rows != null)
                rows.set(stat);
            if (compileInfo != null)
                compileInfo.set(costStat.compileInfo);
            return StatKeys.create(cost, stat, new DistinctKeys<>(costStat.getDistinct(groups, exprStats)));
        }, null, null, null, false, debugInfoWriter);
    }

    // assert что не включает queryJoin
    public <K extends BaseExpr, Z extends Expr> Where getCostPushWhere(final QueryJoin<Z, ?, ?, ?> queryJoin, boolean pushLargeDepth, final UpWheres<WhereJoin> upWheres, final KeyStat keyStat, final StatType type, final Result<Pair<ImRevMap<Z, KeyExpr>, Where>> pushJoinWhere, DebugInfoWriter debugInfoWriter) {
        return and(new WhereJoins(queryJoin)).getInnerCostPushWhere(queryJoin, pushLargeDepth, upWheres, keyStat, type, pushJoinWhere, debugInfoWriter);
    }

    // assert что включает queryJoin
    private <K extends BaseExpr, Z extends Expr> Where getInnerCostPushWhere(final QueryJoin<Z, ?, ?, ?> queryJoin, boolean pushLargeDepth, final UpWheres<WhereJoin> upWheres, final KeyStat keyStat, final StatType type, final Result<Pair<ImRevMap<Z, KeyExpr>, Where>> pushJoinWhere, final DebugInfoWriter debugInfoWriter) {
//        ImSet<BaseExpr> groups = queryJoin.getJoins().values().toSet(); // по идее не надо, так как включает queryJoin
        final Result<PushInfo> pushInfo = new Result<>();
        return calculateCost(SetFact.EMPTY(), false, keyStat, type, (costStat, joinStats, exprStats) -> getCostPushWhere(costStat, queryJoin, pushInfo.result, pushJoinWhere, joinStats, debugInfoWriter), queryJoin, pushInfo, upWheres, pushLargeDepth, debugInfoWriter);
    }
    
    private boolean recProceedChildrenCostWhere(BaseJoin join, MAddExclMap<BaseJoin, Boolean> proceeded, MMap<BaseJoin, MiddleTreeKeep> mMiddleTreeKeeps, MSet<BaseExpr> mAllKeeps, MSet<BaseExpr> mTranslate, boolean keepThis, ImSet<BaseJoin> keepJoins, FunctionSet<BaseJoin> notKeepJoins, ImMap<BaseJoin, ImSet<Edge>> inEdges) {
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
            BaseExpr fromExpr = edge.expr;
            boolean inAllKeep = recProceedCostWhere(fromJoin, proceeded, mMiddleTreeKeeps, mAllKeeps, mTranslate, fromExpr, keepThis, keepJoins.contains(fromJoin), keepJoins, notKeepJoins, inEdges);
            allKeep = inAllKeep && allKeep;
            if(inAllKeep)
                mInAllKeeps.add(fromExpr);
        }

        if(allKeep && join instanceof UnionJoin && ((UnionJoin) join).depends(notKeepJoins)) {
            allKeep = false; // придется перепроверять так как может оказаться не keep не в графе (а проверку не keep не в графе непонятно как делать)
        }

        if(keepThis && !allKeep) // если этот элемент не "полный", значит понадобятся все child'ы для трансляции, соотвественно пометим "полные" из них
            mAllKeeps.addAll(mInAllKeeps.immutable());

        proceeded.exclAdd(join, allKeep);
        return allKeep;
    }

    private boolean recProceedCostWhere(BaseJoin join, MAddExclMap<BaseJoin, Boolean> proceeded, MMap<BaseJoin, MiddleTreeKeep> mMiddleTreeKeeps, MSet<BaseExpr> mAllKeeps, MSet<BaseExpr> mTranslate, BaseExpr upExpr, boolean upKeep, boolean keepThis, ImSet<BaseJoin> keepJoins, FunctionSet<BaseJoin> notKeepJoins, ImMap<BaseJoin, ImSet<Edge>> inEdges) {
        assert keepThis == keepJoins.contains(join);
        if(!keepThis && upKeep && (join instanceof ParamExpr || join instanceof ValueJoin)) // ParamExpr и ValueJoin принудительно делаем keep
            keepThis = true;

        boolean allKeep = recProceedChildrenCostWhere(join, proceeded, mMiddleTreeKeeps, mAllKeeps, mTranslate, keepThis, keepJoins, notKeepJoins, inEdges);

        if (keepThis) // есть верхний keep join, соответственно это его проблема добавить Where (этот сам "подцепится" после этого)
            mMiddleTreeKeeps.add(join, upKeep ? IntermediateKeep.instance : new MiddleTopKeep(upExpr)); // есть ребро "наверх", используем выражение из него
        else
            if (upKeep) // если был keep, а этот не нужен - добавляем трансляцию
                mTranslate.add(upExpr);

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
        public abstract Where getWhere(BaseJoin join, UpWheres<WhereJoin> upWheres, JoinExprTranslator translator);
    }

    private static class MiddleTopKeep extends TopKeep implements MiddleTreeKeep {
        private final BaseExpr expr;

        public MiddleTopKeep(BaseExpr expr) {
            this.expr = expr;
        }

        @Override
        public String toString() {
            return "MIDDLE TREE - " + expr.toString();
        }

        public Where getWhere(BaseJoin join, UpWheres<WhereJoin> upWheres, JoinExprTranslator translator) {
            return JoinExprTranslator.translateExpr(replaceKeyJoinExpr(expr), translator).getWhere(); 
        }
    }

    private static class TopTreeKeep extends TopKeep {
        private static final TopTreeKeep instance = new TopTreeKeep();

        @Override
        public String toString() {
            return "TOP TREE";
        }

        @Override
        public Where getWhere(BaseJoin join, UpWheres<WhereJoin> upWheres, JoinExprTranslator translator) {
            return getUpWhere((WhereJoin) join, upWheres.get((WhereJoin) join), translator);
        }
    }
    
    // при трансляции важно чтобы не появлялись "виртуальные" middle выражения
    // единственным таким выражением является KeyJoinExpr (собственно для этого и создавался), NotNullJoin - top
    private static BaseExpr replaceKeyJoinExpr(BaseExpr expr) {
        if(expr instanceof KeyJoinExpr) {
            return ((KeyJoinExpr) expr).getBaseExpr();
        }
        return expr;        
    }
    
    // it's important that if KeyJoinExpr.getBaseExpr().getBaseJoin() is kept, then and KeyJoinExpr should be kept (in should not happen, but in theory could be)
    // if not, keyJoinExpr may be translated, while KeyJoinExpr.getBaseExpr() kept, and it will break some assertions (for example that allKeep => upJoinWhere = upJoinWhere.translate)
    private static ImSet<BaseJoin> addKeyJoinExprs(ImSet<BaseJoin> set, MAddMap<BaseJoin, Stat> joinStats) {
        // оптимизация
        MExclSet<KeyJoinExpr> mAddedKeyJoinExprs = null;
        for(int i=0,size=joinStats.size();i<size;i++) {
            BaseJoin join = joinStats.getKey(i);
            if (join instanceof KeyJoinExpr && !set.contains(join) && set.contains(((KeyJoinExpr) join).getBaseExpr().getBaseJoin())) {
//                assert false;
                if(mAddedKeyJoinExprs == null)
                    mAddedKeyJoinExprs = SetFact.mExclSetMax(size);
                mAddedKeyJoinExprs.exclAdd((KeyJoinExpr) join);
            }
        }
        if(mAddedKeyJoinExprs != null)
            set = set.addExcl(mAddedKeyJoinExprs.immutable());
        return set;
    }
    
    private static ImSet<BaseExpr> replaceKeyJoinExprs(ImSet<BaseExpr> set) {
        // оптимизация
        boolean foundKeyJoin = false;
        for(BaseExpr expr : set)
            if(expr instanceof KeyJoinExpr) {
                foundKeyJoin = true; 
                break;
            }
        if(!foundKeyJoin)
            return set;

        // повториться по идее не может (так как у KeyJoin один единственный child и либо он сам будет в translate'е либо его child)
        return set.mapSetValues(WhereJoins::replaceKeyJoinExpr);
    }

    private <Z extends Expr> Where getCostPushWhere(CostStat cost, QueryJoin<Z, ?, ?, ?> queryJoin, PushInfo pushInfo, Result<Pair<ImRevMap<Z, KeyExpr>, Where>> pushJoinWhere, final MAddMap<BaseJoin, Stat> joinStats, DebugInfoWriter debugInfoWriter) {
        ImSet<Z> pushedKeys = (ImSet<Z>) cost.getPushKeys(queryJoin);
        if(pushedKeys == null) { // значит ничего не протолкнулось
            // пока падает из-за неправильного computeVertex видимо
//            assert BaseUtils.hashEquals(SetFact.singleton(queryJoin), cost.getJoins());
            return null;
        }
        assert !pushedKeys.isEmpty();
        final ImSet<BaseJoin> keepJoins = addKeyJoinExprs(cost.getJoins().removeIncl(queryJoin), joinStats);
        SFunctionSet<BaseJoin> notKeepJoins = element -> !keepJoins.contains(element) && joinStats.containsKey(element);

        ImList<WhereJoin> adjWheres = pushInfo.upJoins;
        UpWheres<WhereJoin> upWheres = pushInfo.upWheres;
        final ImMap<BaseJoin, ImSet<Edge>> inEdges = pushInfo.edges.group(new BaseUtils.Group<BaseJoin, Edge>() {
            public BaseJoin group(Edge value) {
                return value.getTo();
            }});

        MSet<BaseExpr> mFullExprs = SetFact.mSet();
        MSet<BaseExpr> mTranslate = SetFact.mSet();

        MExclSet<WhereJoin> mTopKeys = SetFact.mExclSetMax(adjWheres.size());
        MMap<BaseJoin, MiddleTreeKeep> mMiddleTreeKeeps = MapFact.mMap(addKeepValue);

        MAddExclMap<BaseJoin, Boolean> proceeded = MapFact.mAddExclMap();
        for(WhereJoin where : adjWheres) { // бежим по upWhere
            boolean keepThis = keepJoins.contains(where);

            recProceedChildrenCostWhere(where, proceeded, mMiddleTreeKeeps, mFullExprs, mTranslate, keepThis, keepJoins, notKeepJoins, inEdges);

            if(keepThis)
                mTopKeys.exclAdd(where);
        }
        // !!! СНАЧАЛА TRANSLATE'М , а потом AND'м, так как Expr'ы могут измениться, тоже самое касается UpWhere - translate'им потом делаем getWhere ??? хотя можно это позже сделать ???
        // UPWHERE, берем все вершины keep у которых нет исходящих в keep (не "промежуточные"), если есть в upWheres берем оттуда, иначе берем первый попавшийся edge у вершины из которой нет выходов (проблема правда в том что InnerFollows не попадут и можно было бы взять класс вместо значения, но это не критично)
        ImSet<BaseExpr> translate = replaceKeyJoinExprs(mTranslate.immutable());
        ImSet<BaseExpr> fullExprs = replaceKeyJoinExprs(mFullExprs.immutable());
        ImRevMap<BaseExpr, KeyExpr> translateKeys = KeyExpr.getMapKeys(translate);
        JoinExprTranslator translator = new JoinExprTranslator(translateKeys, fullExprs);
        ImMap<BaseJoin, MiddleTopKeep> middleTopKeeps = BaseUtils.immutableCast(mMiddleTreeKeeps.immutable().filterFnValues(element -> element instanceof MiddleTopKeep));
        ImMap<BaseJoin, TopKeep> keeps = MapFact.addExcl(mTopKeys.immutable().toMap(TopTreeKeep.instance), middleTopKeeps);
        
        boolean assertCheck = false;
        Where upPushWhere = Where.TRUE();
        for(int i=0,size=keeps.size();i<size;i++) {
            BaseJoin join = keeps.getKey(i);
            TopKeep keep = keeps.getValue(i);

            boolean allKeep = proceeded.get(join);
            Where upJoinWhere = keep.getWhere(join, upWheres, allKeep ? null : translator);

            assert !allKeep || BaseUtils.hashEquals(upJoinWhere, upJoinWhere.translateExpr(translator));
            if(debugInfoWriter != null)
                assertCheck = assertCheck || !(!allKeep || BaseUtils.hashEquals(upJoinWhere, upJoinWhere.translateExpr(translator)));

            upPushWhere = upPushWhere.and(upJoinWhere);
        }

        Result<Where> pushExtraWhere = new Result<>(); // для partition
        ImMap<Z, BaseExpr> queryJoins = queryJoin.getJoins();
        ImMap<Z, Expr> translatedPush = translator.translate(queryJoins.filterIncl(pushedKeys));
        ImMap<Expr, ? extends Expr> translatedPushGroup = queryJoin.getPushGroup(translatedPush, true, pushExtraWhere);
        if(pushExtraWhere.result != null)
            upPushWhere = upPushWhere.and(pushExtraWhere.result.translateExpr(translator));

        KeyExpr hasExprIndexedNoKeys = null;
        if(pushJoinWhere != null && queryJoins.size() == pushedKeys.size()) { // последняя проверка - оптимизация
            assert queryJoin instanceof GroupJoin;
            assert BaseUtils.hashEquals(translatedPush, translatedPushGroup);
            
            // хак для "висячих" ключей, проблема вот в чем : для LIMIT 1 subquery expr (GROUP LAST) оптимизации используется условие проталкивания (для итерации по нему), а этим условием может быть X<=a<=Z (оно не вырезается если внутри не KeyExpr см. removeJoin), соответственно при компиляции этого условия проталкивания будет exception (emptystack)     
            // соответственно ищем ExprIndexedJoin с KeyExpr и без giveNoKeys (по аналогии с removeJoin, именно с ним могут быть проблемы) и если эти KeyExpr в других keepJoins не учавствуют (по аналогии с ExprIndexedJoin.getInnerKeys)
            // теоретически можно и на ExprIndexedJoin с BaseExpr (а не KeyExpr) которые translate'ся проверять, но этот случай при самом проталкивании не учитывается (то есть [=GROUP BY key](f(x)) WHERE Z<=f(x)<=Y )
            ImSet<KeyExpr> innerKeys = null;
            for(BaseJoin keep : keepJoins)
                if(keep instanceof ExprIntervalJoin && ((ExprIntervalJoin)keep).givesNoKeys()) {
                    KeyExpr keyExpr = ((ExprIntervalJoin) keep).getKeyExpr();
                    if(!givesNoKeys(queryJoin, keyExpr)) {
                        if(innerKeys == null)
                            innerKeys = ExprIndexedJoin.getInnerKeys(keepJoins.toArray(new BaseJoin[keepJoins.size()]), (WhereJoin)keep);
                        if(!innerKeys.contains(keyExpr)) {
                            hasExprIndexedNoKeys = keyExpr;
                            break;
                        }                            
                    }                        
                }                
            
            if(hasExprIndexedNoKeys == null) {
                ImRevMap<Z, KeyExpr> mapKeys = KeyExpr.getMapKeys(translatedPush.keys());
                pushJoinWhere.set(new Pair<>(mapKeys, GroupExpr.create(translatedPush, upPushWhere, mapKeys).getWhere()));
            }
        }

        Where where = GroupExpr.create(translatedPushGroup, upPushWhere, translatedPushGroup.keys().toMap()).getWhere();

        if(debugInfoWriter != null)
            debugInfoWriter.addLines("TRANSLATE : " + translateKeys + '\n' + "FULL EXPRS : " + fullExprs + '\n' + "KEEPS : " + keeps + '\n' + "PROCEEDED : " + proceeded + '\n' + "PUSHED INNER WHERE : " + getDetailedWhere(upPushWhere) + " " + assertCheck + '\n' + " PUSHED KEYS : " + pushedKeys + '\n' + "PUSHED GROUP : " + translatedPushGroup + '\n' + "PUSHED WHERE : " + where + '\n' + "PUSHED LAST OPT : " + hasExprIndexedNoKeys + " " + queryJoins.size() + " " + pushedKeys.size());
        
        return where;
    }

    private String getDetailedWhere(Where upPushWhere) {
        return upPushWhere + (upPushWhere instanceof InnerExpr.NotNull ? ((InnerExpr.NotNull) upPushWhere).getInnerDebugJoin().getJoins() + "" : "") + " " + upPushWhere.getOuterKeys();
    }

    private <K extends BaseExpr, Z> CostStat getCost(final QueryJoin pushJoin, final boolean pushLargeDepth, final boolean compileInfo, MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseJoin, Cost> indexedStats, final MAddMap<BaseExpr, PropStat> exprStats, MAddMap<BaseJoin, DistinctKeys> keyDistinctStats, ImSet<Edge> edges, final KeyStat keyStat, final StatType type, DebugInfoWriter debugInfoWriter) {
        CostStat result;
        CostStat pushCost = null;
        assert joinStats.size() > 0;

        List<Collection<Edge<K>>> edgesIn = new ArrayList<>(); // только для lookAhead 

        final GreedyTreeBuilding<BaseJoin, CostStat, Edge<K>> treeBuilding = new GreedyTreeBuilding<>();
        for (int i = 0, size = joinStats.size(); i < size; i++) {
            BaseJoin join = joinStats.getKey(i);

            BitSet inJoins = new BitSet();
            BitSet adjJoins = new BitSet();
            StatKeys statKeys = new StatKeys(indexedStats.get(join), joinStats.getValue(i), keyDistinctStats.get(join));
            JoinCostStat joinCost = new JoinCostStat(join, statKeys, compileInfo, inJoins, adjJoins, pushJoin != null ? new PushCost(statKeys, statKeys.getCost(), true, null) : null);

            if (pushJoin != null && BaseUtils.hashEquals(join, pushJoin))
                pushCost = joinCost;

            treeBuilding.addVertex(join, joinCost);

            int vertexIndex = treeBuilding.getVertexIndex(join); // заполняем матрицу смежности
            inJoins.set(vertexIndex);
            
            edgesIn.add(new ArrayList<>());
        }

        for (Edge edge : edges) {
            treeBuilding.addEdge(edge);

            int fromIndex = treeBuilding.getVertexIndex(edge.getFrom()); // заполняем матрицу смежности
            int toIndex = treeBuilding.getVertexIndex(edge.getTo());
            treeBuilding.getVertexCost(fromIndex).adjJoins.set(toIndex);
            treeBuilding.getVertexCost(toIndex).adjJoins.set(fromIndex);
            
            edgesIn.get(toIndex).add(edge);
        }

        // только для lookAhead
        List<CostStat> costs = treeBuilding.getVertexCosts();
        List<Collection<Edge<K>>> edgesOut = treeBuilding.getAdjList();
        
        // отдельно считаем cost
        final GreedyTreeBuilding.CalculateCost<BaseJoin, CostStat, Edge<K>> costCalc = getCostFunc(pushJoin, exprStats, compileInfo, keyStat, type, costs, edgesOut, edgesIn, debugInfoWriter != null);
        treeBuilding.debugInfoWriter = debugInfoWriter;
        
        GreedyTreeBuilding.TreeNode<BaseJoin, CostStat> compute;
        if (pushJoin != null) {
            assert joinStats.containsKey(pushJoin);
            if(joinStats.size() == 1)
                return pushCost;

            compute = treeBuilding.computeWithVertex(pushJoin, costCalc, (a, b) -> a.pushCompareTo(b, pushJoin, pushLargeDepth));
        } else
            compute = treeBuilding.compute(costCalc);
        result = compute.node.getCost();

        if(pushJoin != null && pushCost.pushCompareTo(result, pushJoin, pushLargeDepth || pushJoin instanceof LastJoin) <= 0) // так как текущий computeWithVertex всегда берет хоть одно ребро, последняя проверка нужна так как есть оптимизация быстрой остановки когда cost становится равным Max, выходить - а эта проверка пессимистична (пытается протолкнуть даже при совпадении cost'ов) 
            return pushCost;
        else
            return result;
    }

    private static MergeCostStat max = new MergeCostStat(null, null, null, null, null, null, null, null, 0
                                                            , null
                                                        );

    private static <K extends BaseExpr, Z> GreedyTreeBuilding.CalculateCost<BaseJoin, CostStat, Edge<K>> getCostFunc(final QueryJoin pushJoin, final MAddMap<BaseExpr, PropStat> exprStats, final boolean compileInfo, final KeyStat keyStat, final StatType type, final List<CostStat> costs, final List<Collection<Edge<K>>> edgesOut, final List<Collection<Edge<K>>> edgesIn, final boolean debugEnabled) {
        return new GreedyTreeBuilding.CalculateCost<BaseJoin, CostStat, Edge<K>>() {

                @Override
                public CostStat calculateLowerBound(GreedyTreeBuilding.Node<BaseJoin, CostStat> a, GreedyTreeBuilding.Node<BaseJoin, CostStat> b, Iterable<Edge<K>> edges) {

//                    if(!useLowerBound) return new MergeCostStat(Cost.MIN, Stat.MIN, Cost.ALOT, Stat.MIN, Stat.MIN, -1, null, null, null, null, null);

                    CostStat aCostStat = a.getCost();
                    CostStat bCostStat = b.getCost();

                    if(!aCostStat.adjacentWithCommon(bCostStat))
                        return max;

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

                    ImList<Edge<K>> edgesList = null;
                    Stat[] aEdgeStats = null;
                    Stat[] bEdgeStats = null;

                    Stat newStat;
                    if(!edges.iterator().hasNext()) { // оптимизация, как самый самый самый частый случай
                        newStat = aAdjStat.mult(bAdjStat);
                    } else {
                        edgesList = ListFact.toList(edges);
                        int size = edgesList.size();

                        aEdgeStats = new Stat[size];
                        bEdgeStats = new Stat[size];

                        for (int i = 0; i < size; i++) {
                            Edge<K> edge = edgesList.get(i);
                            BaseExpr expr = edge.expr;
                            boolean aIsKey = aCostStat.getJoins().contains(edge.getTo()); // A - ключ
                            if (aIsKey) {
                                PropStat bEdgeStat = bCostStat.getPropStat(expr, exprStats);
                                if (bEdgeStat.notNull != null)
                                    bAdjStat = bAdjStat.min(bEdgeStat.notNull);
                                bEdgeStats[i] = bEdgeStat.distinct;
                                aEdgeStats[i] = aCostStat.getKeyStat(edge);
                            } else {
                                PropStat aEdgeStat = aCostStat.getPropStat(expr, exprStats);
                                if (aEdgeStat.notNull != null)
                                    aAdjStat = aAdjStat.min(aEdgeStat.notNull);
                                aEdgeStats[i] = aEdgeStat.distinct;
                                bEdgeStats[i] = bCostStat.getKeyStat(edge);
                            }
                        }

                        newStat = calcEstJoinStat(aAdjStat, bAdjStat, size, aEdgeStats, bEdgeStats, true, null, null, null);
                    }

                    Cost aCost = aCostStat.getCost();
                    Cost bCost = bCostStat.getCost();
                    Cost newCost = (b.getVertex() != null ? aCost.min(bCost) : aCost.or(bCost)).or(new Cost(newStat)); // если есть vertex - может протолкнуться иначе нет
                    BitSet newInJoins = or(aCostStat.inJoins, bCostStat.inJoins);
                    BitSet newAdjJoins = orRemove(aCostStat.adjJoins, bCostStat.adjJoins, newInJoins);

                    return new MergeCostStat(newCost, newStat, newInJoins, newAdjJoins,
                            aCost, bCost, aAdjStat, bAdjStat, aCostStat.getJoins().size() + bCostStat.getJoins().size()
                            , debugEnabled ? new MergeCostStat.DebugInfo(aCostStat, bCostStat, aEdgeStats, bEdgeStats, edgesList) : null
                    );
                }

                public CostStat calculate(GreedyTreeBuilding.Node<BaseJoin, CostStat> a, GreedyTreeBuilding.Node<BaseJoin, CostStat> b, Iterable<Edge<K>> edges) {

                    // берем 2 вершины
                    CostStat aCostStat = a.getCost();
                    CostStat bCostStat = b.getCost();
//                    assert aCostStat.adjJoins.intersects(bCostStat.adjJoins);
                    if (aCostStat.compareTo(bCostStat) > 0) { // будем считать что у a cost меньше то есть он "левый"
                        GreedyTreeBuilding.Node<BaseJoin, CostStat> t = a;
                        a = b;
                        b = t;
                        CostStat tCost = aCostStat;
                        aCostStat = bCostStat;
                        bCostStat = tCost;
                    }
                    
                    assert BaseUtils.nullHashEquals(bCostStat.getJoin(), b.getVertex());
                    MergeCostStat result = calculateCost(aCostStat, bCostStat, edges); // assert 
                    
                    // LOOK AHEAD для декартового произведения
                    if(!aCostStat.adjacent(bCostStat) && aCostStat.adjacentCommon(bCostStat)) { // последняя проверка нужна, так как если не осталось adjacentWithCommon, начинают выбираться "чистые" декартовы произведения
                        BitSet common = and(aCostStat.adjJoins, bCostStat.adjJoins);

                        Cost lookAheadCost = lookAheadCost(result, common);
                        if(lookAheadCost != null) {
                            assert result.getCost().rows.less(lookAheadCost.rows);
                            result = new MergeCostStat(lookAheadCost, result);
                        }
                    }

                    return result;                    
                }

                Cost lookAheadCost(MergeCostStat result, BitSet commonJoins) {
                    final ImSet<BaseJoin> resultJoins = result.getJoins();
    
                    // предварительная оптимизация
                    Cost minPushedCost = null;
                    for (int i = commonJoins.nextSetBit(0); i != -1; i = commonJoins.nextSetBit(i + 1)) {
                        CostStat leafCostStat = costs.get(i);
                        if (leafCostStat.compareTo(result) > 0) { // по идее обратного быть не должно, так как скорее всего это соединение должно было быть выбрано а не декартово произведение
                            Iterable<Edge<K>> filteredEdges = BaseUtils.mergeIterables(
                                BaseUtils.filterIterable(edgesOut.get(i), (SFunctionSet<Edge<K>>) element -> resultJoins.contains(element.getTo())), BaseUtils.filterIterable(edgesIn.get(i), (SFunctionSet<Edge<K>>) element -> resultJoins.contains(element.getFrom())));
                            Cost pushedCost = calculateCost(result, leafCostStat, filteredEdges).getCost();
                            
                            if(result.getCost().rows.less(pushedCost.rows)) {
                                minPushedCost = minPushedCost == null ? pushedCost : minPushedCost.min(pushedCost); 
                            } else
                                return null;
                        }
                    }
                    return minPushedCost;
                }
    
                MergeCostStat calculateCost(CostStat aCostStat, CostStat bCostStat, Iterable<Edge<K>> edges) {
                    assert aCostStat.compareTo(bCostStat) <= 0;
                    BaseJoin<Z> bv = bCostStat.getJoin();
                            
                    Cost aCost = aCostStat.getCost(); // не предполагает изменение
                    Cost bBaseCost = bCostStat.getCost();
                    AddValue<BaseExpr, Stat> minStat = minStat();

                    // обрабатываем notNull'ы, важно чтобы идеологически совпадал с getPushedCost
                    ImList<Edge<K>> edgesList = ListFact.toList(edges);
                    final int edgesCount = edgesList.size();
                    Stat[] aEdgeStats = new Stat[edgesCount];
                    Stat[] bEdgeStats = new Stat[edgesCount];
                    Stat[] aNotNullStats = new Stat[edgesCount];
                    boolean[] aIsKeys = new boolean[edgesCount];
                    Stat aAdjStat = aCostStat.getStat();
                    Stat bAdjStat = bCostStat.getStat();

                    BaseJoin[] keyJoins = compileInfo ? new BaseJoin[edgesCount] : null;
                    Object[] keys = new Object[edgesCount];
                    
                    MAddMap<BaseExpr, Integer> minProps = MapFact.mAddMapMax(edgesCount, MapFact.override()); // map with indexes on edges with min stat

                    // читаем edge'и
                    for (int j=0;j<edgesCount;j++) {
                        Edge<K> edge = edgesList.get(j);
                        BaseExpr expr = edge.expr;
                        
                        boolean aIsKey = aCostStat.getJoins().contains(edge.getTo()); // A - ключ
                        aIsKeys[j] = aIsKey;
                        
                        Integer minPropIndex = minProps.get(expr);
                        assert minPropIndex == null || aIsKeys[minPropIndex] == aIsKey; // if there is one more same expr, direction of edge will be alse the same

                        if (aIsKey) {
                            Stat aEdgeStat = aCostStat.getKeyStat(edge);
                            aEdgeStats[j] = aEdgeStat;

                            if(minPropIndex == null) { // optimization
                                PropStat bEdgeStat = bCostStat.getPropStat(expr, exprStats);
                                if (bEdgeStat.notNull != null) 
                                    bAdjStat = bAdjStat.min(bEdgeStat.notNull);
                                bEdgeStats[j] = bEdgeStat.distinct;
                                
                                minProps.add(expr, j);
                            } else {
                                bEdgeStats[j] = bEdgeStats[minPropIndex];
                                
                                if(aEdgeStat.less(aEdgeStats[minPropIndex]))
                                    minProps.add(expr, j);
                            }

                            if(compileInfo)
                                keyJoins[j] = edge.join;
                        } else {
                            Stat bEdgeStat = bCostStat.getKeyStat(edge);
                            bEdgeStats[j] = bEdgeStat;

                            if(minPropIndex == null) { // optimization
                                PropStat aEdgeStat = aCostStat.getPropStat(expr, exprStats);
                                if (aEdgeStat.notNull != null) 
                                    aAdjStat = aAdjStat.min(aEdgeStat.notNull);
                                aEdgeStats[j] = aEdgeStat.distinct;
                                aNotNullStats[j] = aEdgeStat.notNull;

                                minProps.add(expr, j);
                            } else {
                                aEdgeStats[j] = aEdgeStats[minPropIndex];
                                aNotNullStats[j] = aNotNullStats[minPropIndex];

                                if(bEdgeStat.less(bEdgeStats[minPropIndex]))
                                    minProps.add(expr, j);
                            }

                            keys[j] = edge.key;
                        }
                    }
                    int minPropsCount = minProps.size();

                    // PUSH COST (STATS)
                    if(bv != null && (!edgesList.isEmpty() || pushJoin != null)) { // последнее - оптимизация

                        MExclMap<Z, Stat> mKeys = MapFact.mExclMapMax(edgesCount);
                        MExclMap<Z, Stat> mNotNullKeys = MapFact.mExclMapMax(edgesCount);
                        for (int i = 0; i < edgesCount; i++) {
                            if (!aIsKeys[i]) {
                                Z key = (Z)keys[i];
                                mKeys.exclAdd(key, aEdgeStats[i]);
                                if(aNotNullStats[i] != null)
                                    mNotNullKeys.exclAdd(key, aNotNullStats[i]);
                            }
                        }

                        JoinCostStat<Z> bJoinCost = (JoinCostStat<Z>) bCostStat;
                        assert BaseUtils.hashEquals(bv, bJoinCost.join);

                        ImMap<Z, Stat> pushKeys = mKeys.immutable();
                        ImMap<Z, Stat> pushNotNullKeys = mNotNullKeys.immutable();
                        Stat aStat = aCostStat.getStat();

                        ImSet<BaseExpr> usedNotNulls = compileInfo ? SetFact.EMPTY() : null;                        
                        StatKeys<Z> pushedStatKeys;
                        Result<ImSet<Z>> rPushedKeys = pushJoin != null ? new Result<>() : null;
                        Result<ImSet<BaseExpr>> rPushedProps = compileInfo ? new Result<>() : null;
                        if (bv instanceof QueryJoin) { // для query join можно протолкнуть внутрь предикаты
                            pushedStatKeys = ((QueryJoin) bv).getPushedStatKeys(type, aCost, aStat, pushKeys, pushNotNullKeys, rPushedKeys);

                            pushedStatKeys = pushedStatKeys.min(bJoinCost.statKeys); // по идее push должен быть меньше, но из-за несовершенства статистики и отсутствия проталкивания в таблицу (pushedJoin присоединятся к маленьким join'ам и может немного увеличивать cost / stat), после "проталкивания в таблицу" можно попробовать вернуть assertion
//                                assert BaseUtils.hashEquals(pushedStatKeys.min(bJoinCost.statKeys), pushedStatKeys);

                            for(int i=0;i<edgesCount;i++) // обновляем bEdgeStats
                                if(!aIsKeys[i])
                                    bEdgeStats[i] = pushedStatKeys.getDistinct((Z)keys[i]);
                            bAdjStat = bAdjStat.min(pushedStatKeys.getRows());
                        } else {
                            MExclMap<BaseExpr, Stat> mProps = MapFact.mExclMapMax(minPropsCount);
                            MSet<BaseExpr> mNotNullProps = compileInfo ? SetFact.mSetMax(minPropsCount) : null;
                            for (int p = 0; p < minPropsCount ; p++) {
                                int i = minProps.getValue(p);
                                if (aIsKeys[i]) {
                                    BaseExpr expr = minProps.getKey(p);
                                    mProps.exclAdd(expr, aEdgeStats[i]);

                                    if (compileInfo) {
                                        BaseJoin to = keyJoins[i];
                                        if (to instanceof ExprStatJoin && ((ExprStatJoin) to).notNull)
                                            mNotNullProps.add(expr);
                                    }
                                }
                            }
                            ImMap<BaseExpr, Stat> pushProps = mProps.immutable();

                            Cost pushedCost = bv.getPushedCost(keyStat, type, aCost, aStat, pushKeys, pushNotNullKeys, pushProps, rPushedKeys, rPushedProps); // впоследствие можно убрать aStat добавив predicate pushDown таблицам

                            pushedCost = pushedCost.min(bJoinCost.getCost()); // по идее push должен быть меньше, но из-за несовершенства статистики и отсутствия проталкивания в таблицу (pushedJoin присоединятся к маленьким join'ам и может немного увеличивать cost / stat), после "проталкивания в таблицу" можно попробовать вернуть assertion
//                                assert bv instanceof KeyExpr || BaseUtils.hashEquals(pushedCost.min(bJoinCost.getCost()), pushedCost); // по идее push должен быть меньше

                            pushedStatKeys = bJoinCost.statKeys.replaceCost(pushedCost); // подменяем только cost
                            if (compileInfo && rPushedProps.result != null) // только notNull и реально использовался для уменьшения cost'а в таблице
                                usedNotNulls = mNotNullProps.immutable().filter(rPushedProps.result);
                            assert bAdjStat.lessEquals(pushedStatKeys.getRows());//
                        }
    
                        bCostStat = new JoinCostStat<>(bv, pushedStatKeys, usedNotNulls, compileInfo, bJoinCost.inJoins, bJoinCost.adjJoins, 
                                            pushJoin != null ? new PushCost(pushedStatKeys, aCost, false, rPushedKeys.result) : null); // теоретически можно и все ребра (в смысле что предикаты лишними не бывают ???)
                    }
    
                    // STAT ESTIMATE
                    Result<Stat> rAAdjStat = new Result<>();
                    Result<Stat> rBAdjStat = new Result<>();
                    Stat newStat = calcEstJoinStat(aAdjStat, bAdjStat, minPropsCount, aEdgeStats, bEdgeStats, true, rAAdjStat, rBAdjStat, minProps); // removing duplicate exprs (after push, because in push that exprs matter)
                    aAdjStat = rAAdjStat.result;
                    bAdjStat = rBAdjStat.result;
                    Cost newCost = aCost.or(bCostStat.getCost()).or(new Cost(newStat));
    
                    ImMap<BaseJoin, DistinctKeys> newKeyStats = aCostStat.getKeyStats().addExcl(bCostStat.getKeyStats());
                    ImMap<BaseJoin, Stat> newJoinStats = reduceIntermediateStats(newStat.min(aAdjStat), aCostStat).addExcl(reduceIntermediateStats(newStat.min(bAdjStat), bCostStat)); // также фильтруем по notNull
                    CompileInfo newCompileInfo = compileInfo ? aCostStat.compileInfo.add(bCostStat.compileInfo) : null;
                    BitSet newInJoins = or(aCostStat.inJoins, bCostStat.inJoins);
                    BitSet newAdjJoins = orRemove(aCostStat.adjJoins, bCostStat.adjJoins, newInJoins);
                    ImMap<QueryJoin, PushCost> newPushCosts = pushJoin != null ? CostStat.addPushCosts(aCostStat.pushCosts, bCostStat.pushCosts) : null;
    
                    MExclMap<BaseExpr, Stat> mPropAdjStats = MapFact.mExclMapMax(minPropsCount); // ключи не считаем, так как используются один раз. NotNull'ы не нужны, так как статистика уже редуцировалась
                    for (int p = 0 ; p < minPropsCount; p++) {
                        int i = minProps.getValue(p);
                        
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
                            mPropAdjStats.exclAdd(minProps.getKey(p), keyStat);
                    }
                    ImMap<BaseExpr, Stat> newPropStats = aCostStat.getPropStats().addExcl(bCostStat.getPropStats()).merge(mPropAdjStats.immutable(), minStat);
    
                    return new MergeCostStat(newCost, null, newStat, newInJoins, newAdjJoins,
                            aCost, bBaseCost, aAdjStat, bAdjStat, newJoinStats.size(),
                            newJoinStats, newKeyStats, newPropStats, newPushCosts, newCompileInfo
                            , debugEnabled ? new MergeCostStat.DebugInfo(aCostStat, bCostStat, aEdgeStats, bEdgeStats, edgesList) : null
                                            );
                }
        };
    }

    private static BitSet or(BitSet a, BitSet b) {
        BitSet result = (BitSet) a.clone();
        result.or(b);
        return result;
    }

    private static BitSet and(BitSet a, BitSet b) {
        BitSet result = (BitSet) a.clone();
        result.and(b);
        return result;
    }
    
    private static BitSet orRemove(BitSet a, BitSet b, BitSet remove) {
        BitSet result = (BitSet) a.clone();
        result.or(b);
        result.andNot(remove);
        return result;
    }

    // точка входа чтобы обозначить необходимую общую логику estimate'ов Push Cost (пока в Table) и Stat (в общем случае)
    public static Stat calcEstJoinStat(Stat aStat, Stat bStat, int edgesCount, Stat[] aEdgeStats, Stat[] bEdgeStats, boolean adjStat, Result<Stat> rAAdjStat, Result<Stat> rBAdjStat, MAddMap<BaseExpr, Integer> minProps) {

        Stat totalStatReduce = Stat.ONE; // По умолчанию cost = MAX(a,b), stat = MAX((a + b)/(S(MAX(dist.from, dist.to))), MIN(a,b)), где dist.from = MIN(a.stat, e.dist.from), dist.to = MIN(b.stat, e.dist.to)

        Stat aReduce = Stat.ONE;
        Stat bReduce = Stat.ONE;
        for (int p = 0; p < edgesCount; p++) {
            int i = minProps != null ? minProps.getValue(p) : p;
            
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

        return joinStats.mapValues(value -> value.min(newStat));
    }

    private <K extends BaseExpr> void buildGraphWithStats(ImSet<K> groups, Result<ImSet<Edge>> edges, MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, PropStat> exprStats, MAddMap<Edge, Stat> keyStats,
                                                          MAddMap<BaseJoin, DistinctKeys> keyDistinctStats, MAddMap<BaseJoin, Cost> indexedStats, StatType statType, KeyStat keyStat, QueryJoin keepIdentJoin, Result<PushInfo> pushInfo, UpWheres<WhereJoin> pushUpWheres) {

        Result<ImSet<BaseExpr>> exprs = new Result<>();
        Result<ImSet<BaseJoin>> joins = new Result<>();
        
        buildGraph(groups, edges, exprs, joins, keyStat, statType, keepIdentJoin, pushInfo, pushUpWheres);

        // раньше было слияние expr'ов, которые входят в одни и те же join'ы, по идее это уменьшает кол-во двудольных графов и сильно помогает getMSTExCost
        // но если мы их сольем изначально, то (a1,b1) и (a2,b2) сольются в (a1 + a2, b1+b2) и мы можем потерять важную информацию, раньше же это делалось параллельно с балансировкой, но это очень сильно усложняло архитектуру и не вязалось с получением информации для pushDown'а
        // mergeCrossColumns();

        buildStats(joins, exprs, edges.result, joinStats, exprStats, keyStats, keyDistinctStats, indexedStats, statType, keyStat);
    }

    private void buildStats(Result<ImSet<BaseJoin>> joins, Result<ImSet<BaseExpr>> exprs, ImSet<Edge> edges, MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, PropStat> exprStats, MAddMap<Edge, Stat> keyStats, MAddMap<BaseJoin, DistinctKeys> keyDistinctStats, MAddMap<BaseJoin, Cost> indexedStats, final StatType statType, final KeyStat keyStat) {

        ImMap<BaseJoin, StatKeys> joinStatKeys = joins.result.mapValues((BaseJoin value) -> value.getStatKeys(keyStat, statType, false));

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

    private static void addExpr(BaseJoin join, MSet<BaseJoin> mJoins, Queue<BaseJoin> queue, QueryJoin keepIdentJoin) {
        if(keepIdentJoin != null && BaseUtils.hashEquals(join, keepIdentJoin))
            join = keepIdentJoin;
        if(!mJoins.add(join))
            queue.add(join);
    }

    private ImList<WhereJoin> getAdjIntervalWheres(Result<UpWheres<WhereJoin>> upAdjWheres, QueryJoin excludeQueryJoin) {
        // в принципе в cost based это может быть не нужно, просто нужно сделать result cost и stat объединения двух ExprIndexedJoin = AverageIntervalStat и тогда жадняк сам разберется
        boolean hasExprIndexed = false; // оптимизация
        WhereJoin[] wheres = getAdjWheres();
        for(WhereJoin valueJoin : wheres)
            if(valueJoin instanceof ExprIndexedJoin && ((ExprIndexedJoin) valueJoin).isNotNull()) {
                hasExprIndexed = true;
                break;
            }
        if(!hasExprIndexed)
            return ListFact.toList(wheres);

        MList<WhereJoin> mResult = ListFact.mList();

        MExclSet<ExprIndexedJoin> mExprIndexedJoins = SetFact.mExclSet();
        for(WhereJoin valueJoin : wheres)
            if(valueJoin instanceof ExprIndexedJoin && ((ExprIndexedJoin) valueJoin).isNotNull())  // нельзя в ExprStatJoin при not преобразовывать, так как ExprStat => notNull, а ExprIndexedJoin, not не => notNull
                mExprIndexedJoins.exclAdd((ExprIndexedJoin) valueJoin);
            else
                mResult.add(valueJoin);

        ExprIndexedJoin.fillIntervals(mExprIndexedJoins.immutable(), mResult, upAdjWheres, wheres, excludeQueryJoin);
        return mResult.immutableList();
    }

    private WhereJoin[] getAdjWheres() {
        return wheres;
    }
    
    private static class PushInfo {
        
        public final ImList<WhereJoin> upJoins;
        public final UpWheres<WhereJoin> upWheres; 
        public final ImSet<Edge> edges;

        public PushInfo(ImList<WhereJoin> upJoins, UpWheres<WhereJoin> upWheres, ImSet<Edge> edges) {
            this.upJoins = upJoins;
            this.upWheres = upWheres;
            this.edges = edges;
        }
    }

    private <K extends BaseExpr> void buildGraph(ImSet<K> groups, Result<ImSet<Edge>> rEdges, Result<ImSet<BaseExpr>> exprs, Result<ImSet<BaseJoin>> joins, KeyStat keyStat, StatType statType, QueryJoin keepIdentJoin, Result<PushInfo> pushInfo, UpWheres<WhereJoin> pushUpWheres) {
        MExclSet<Edge> mEdges = SetFact.mExclSet();
        MSet<BaseExpr> mExprs = SetFact.mSet();
        MSet<BaseJoin> mJoins = SetFact.mSet();
        Queue<BaseJoin> queue = new LinkedList<>();

        // для того чтобы сгруппировать одинаковые expr и тем самым создать виртуальную связь key-key (без join'а самого expr'а)
        MAddExclMap<BaseExpr, MAddCol<Pair<BaseJoin<Object>, Object>>> joinExprEdges = MapFact.mAddExclMap();

        // собираем все ребра и вершины (без ExprIndexedJoin они все равно не используются при подсчете статистики, но с интервалами)
        Result<UpWheres<WhereJoin>> upAdjWheres = pushInfo != null ? new Result<>(pushUpWheres) : null;         
        ImList<WhereJoin> adjIntervalWheres = getAdjIntervalWheres(upAdjWheres, keepIdentJoin);
        for(WhereJoin valueJoin : adjIntervalWheres)
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

                InnerBaseJoin<?> exprJoin = joinExpr.getBaseJoin();
                
                if(exprJoin.getJoins().isEmpty()) { // оптимизация (хотя и не до конца правильная если скажем это GroupJoin без параметров, но большим cost'ом)
                    addExpr(mEdges, mExprs, join, joinKey, joinExpr);
                } else {
                    MAddCol<Pair<BaseJoin<Object>, Object>> exprEdges = joinExprEdges.get(joinExpr);
                    if (exprEdges == null) {
                        exprEdges = ListFact.mAddCol();
                        joinExprEdges.exclAdd(joinExpr, exprEdges);
                    }
                    exprEdges.add(new Pair<>(join, joinKey));
                }

                addQueueJoin(exprJoin, mJoins, queue, keepIdentJoin);
            }
        }

        for(int i=0,size=joinExprEdges.size();i<size;i++) {
            BaseExpr joinExpr = joinExprEdges.getKey(i);
            MAddCol<Pair<BaseJoin<Object>, Object>> exprEdges = joinExprEdges.getValue(i);

            Pair<BaseJoin<Object>, Object> singleEdge = null; // оптимизация, чтобы не создавать не нужные вершины
            BaseExpr singleExpr = null;            
            KeyJoinExpr keyJoinExpr = null;
            for(int j=0,sizeJ=exprEdges.size();j<sizeJ;j++) {
                Pair<BaseJoin<Object>, Object> exprEdge = exprEdges.get(j);
                if(exprEdge.first instanceof ExprJoin && !((ExprJoin)exprEdge.first).canBeKeyJoined()) { // не создаем промежуточную вершину, чтобы не протолкнулся висячий ключ
                    addExpr(mEdges, mExprs, exprEdge.first, exprEdge.second, joinExpr);
                } else {
                    if(singleEdge == null ) {
                        singleEdge = exprEdge;
                        singleExpr = joinExpr;
                    } else {
                        if(keyJoinExpr == null)
                            keyJoinExpr = new KeyJoinExpr(joinExpr);
                        singleExpr = keyJoinExpr;
                        addExpr(mEdges, mExprs, exprEdge.first, exprEdge.second, keyJoinExpr);
                    }
                }                    
            }
            
            if(keyJoinExpr != null) {
                KeyJoinExpr baseJoin = keyJoinExpr.getBaseJoin();
                mJoins.add(baseJoin);
                addExpr(mEdges, mExprs, baseJoin, 0, joinExpr);
            }            
            if(singleEdge != null)
                addExpr(mEdges, mExprs, singleEdge.first, singleEdge.second, singleExpr);
        }

        exprs.set(mExprs.immutable());

        // добавляем notNull статистику
        for(Expr expr : exprs.result) {
            if(expr instanceof InnerExpr) {
                InnerExpr innerExpr = (InnerExpr) expr;
                ExprStatJoin notNullJoin = innerExpr.getNotNullJoin(keyStat, statType);
                if (notNullJoin != null && !mJoins.add(notNullJoin)) {
                    if(pushInfo != null) {
                        adjIntervalWheres = adjIntervalWheres.addList(notNullJoin);
                        upAdjWheres.set(new UpWheres<>(upAdjWheres.result.addExcl(notNullJoin, innerExpr.getUpNotNullWhere())));
                    }
                    mEdges.exclAdd(new Edge(notNullJoin, 0, innerExpr));
                }
            }
        }

        joins.set(mJoins.immutable());
        ImSet<Edge> edges = mEdges.immutable();
        rEdges.set(edges);
        
        if(pushInfo != null) 
            pushInfo.set(new PushInfo(adjIntervalWheres, upAdjWheres.result, edges));
    }

    private static void addExpr(MExclSet<Edge> mEdges, MSet<BaseExpr> mExprs, BaseJoin<Object> join, Object joinKey, BaseExpr joinExpr) {
        Edge edge = new Edge(join, joinKey, joinExpr);
        mEdges.exclAdd(edge);
        mExprs.add(joinExpr);
    }

    private static Where getUpWhere(WhereJoin join, UpWhere upWhere, JoinExprTranslator translator) {
        Where result = Where.TRUE();
        for(BaseExpr baseExpr : ((BaseJoin<?>) join).getJoins().valueIt()) {
            Expr expr = JoinExprTranslator.translateExpr(baseExpr, translator);
            Where where;
            if(expr instanceof BaseExpr)
                where = ((BaseExpr) expr).getOrWhere();
            else
                where = expr.getWhere(); // orWhere не получим, будет избыточно (andWhere лишний раз проand'ся), но пока другие варианты не понятны
            result = result.and(where);
        }
        return upWhere.getWhere(translator).and(result);
    }

    public <K extends BaseExpr, Z extends Expr> Where getCostPushWhere(final QueryJoin<Z, ?, ?, ?> queryJoin, boolean pushLargeDepth, UpWheres<WhereJoin> upWheres, KeyStat keyStat, final StatType type, KeyEqual keyEqual, DebugInfoWriter debugInfoWriter) {
        WhereJoins adjWhereJoins = this;
        if(!keyEqual.isEmpty()) { // для оптимизации
            Result<UpWheres<WhereJoin>> equalUpWheres = new Result<>();
            WhereJoins equalWhereJoins = keyEqual.getWhereJoins(equalUpWheres);
            adjWhereJoins = adjWhereJoins.and(equalWhereJoins);
            upWheres = adjWhereJoins.andUpWheres(upWheres, equalUpWheres.result); // можно было бы по идее просто слить addExcl, но на всякий случай сделаем в общем случае
            keyStat = keyEqual.getKeyStat(keyStat);
        }
        return adjWhereJoins.getCostPushWhere(queryJoin, pushLargeDepth, upWheres, keyEqual.getKeyStat(keyStat), type, (Result<Pair<ImRevMap<Z,KeyExpr>,Where>>) null, debugInfoWriter);
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, final KeyStat keyStat, StatType type, final KeyEqual keyEqual) {
        if(!keyEqual.isEmpty()) { // для оптимизации
            return and(keyEqual.getWhereJoins()).getStatKeys(groups, keyEqual.getKeyStat(keyStat), type);
        } else
            return getStatKeys(groups, keyStat, type);
    }

    // there is an optimization if nothing was removed return null
    public static <T extends WhereJoin, K extends Expr> WhereJoins removeJoin(QueryJoin<K, ?, ?, ?> removeJoin, WhereJoin[] wheres, UpWheres<WhereJoin> upWheres, Result<UpWheres<WhereJoin>> resultWheres) {
        WhereJoins result = null;
        UpWheres<WhereJoin> resultUpWheres = null;
        MExclSet<WhereJoin> mKeepWheres = SetFact.mExclSetMax(wheres.length); // массивы
        for(WhereJoin<?, ?> whereJoin : wheres) {
            WhereJoins removeJoins;
            Result<UpWheres<WhereJoin>> removeUpWheres = new Result<>();

            boolean remove = BaseUtils.hashEquals(removeJoin, whereJoin);
            InnerJoins joinFollows = null;
            if (!remove && whereJoin instanceof ExprStatJoin && ((ExprStatJoin) whereJoin).depends(removeJoin)) // без этой проверки может бесконечно проталкивать
                remove = true;

            // жесткий хак, вообще нужно проталкивать все предикаты, но в случае (GROUP BY d) WHERE f(d) AND a=<d<=b, может протолкнутся a<=d<=b без f(d) и получится висячий ключ
            // собственно нужно сделать чтобы предикат a<=d<=b - при отсутствии ключа добавлял сам join на iterate(a,d,b), но пока этого не сделано, такой хак: 
            if (!remove && whereJoin instanceof ExprIndexedJoin && ((ExprIndexedJoin)whereJoin).givesNoKeys()) {
                KeyExpr keyExpr = ((ExprIndexedJoin) whereJoin).getKeyExpr();
                if(givesNoKeys(removeJoin, keyExpr))
                    remove = true;
            }
            // all lower checks should match calculateOrWhere 
            if(!remove && whereJoin instanceof PartitionJoin) { // we don't need to check it in joinFollows, since PartitionExpr is InnerExpr
                if(UnionJoin.depends(((PartitionJoin) whereJoin).getOrWhere(), removeJoin))
                    remove = true;
            }
            if(!remove) {
                Result<UpWheres<InnerJoin>> rJoinUpWheres = new Result<>();
                Result<Boolean> unionRemoved = new Result<>(false); // need this because of null optimization
                joinFollows = whereJoin.getJoinFollows(rJoinUpWheres, unionJoin -> {
                    if (unionJoin.depends(removeJoin)) { // without this check there can be infinite push down, so we just eliminate union join branches that depends on removeJoin (but only branches, to avoid removing significant branches)
                        unionRemoved.set(true);
                        return true;
                    }
                    return false;
                });
                UpWheres<WhereJoin> joinUpWheres = BaseUtils.immutableCast(rJoinUpWheres.result);
                removeJoins = joinFollows.removeJoin(removeJoin, joinUpWheres, removeUpWheres);
                if(removeJoins == null && unionRemoved.result) {
                    removeJoins = joinFollows.getWhereJoins();
                    removeUpWheres.set(joinUpWheres);
                }
            } else {
                removeJoins = WhereJoins.EMPTY;
                removeUpWheres.set(UpWheres.EMPTY());
            }

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

    // if joins есть has key in value and not key in expr
    public static <K extends Expr> boolean givesNoKeys(QueryJoin<K, ?, ?, ?> removeJoin, KeyExpr keyExpr) {
        if(keyExpr != null) { // может быть not
            ImMap<K, BaseExpr> joins = removeJoin.getJoins();
            for(int i=0,size=joins.size();i<size;i++)
                if(BaseUtils.hashEquals(keyExpr, joins.getValue(i)) && !(joins.getKey(i) instanceof KeyExpr))
                    return false;
        }
        return true;
    }

    // устраняет сам join чтобы при проталкивании не было рекурсии
    public WhereJoins removeJoin(QueryJoin join, UpWheres<WhereJoin> upWheres, Result<UpWheres<WhereJoin>> resultWheres) {
        return removeJoin(join, wheres, upWheres, resultWheres); // не надо Bet
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
    public <K extends Expr, T extends Expr> Where getPushWhere(UpWheres<WhereJoin> upWheres, QueryJoin<K, ?, ?, ?> pushJoin, boolean pushLargeDepth, boolean isInner, KeyStat keyStat, Result<Pair<ImRevMap<K, KeyExpr>, Where>> pushJoinWhere, DebugInfoWriter debugInfoWriter) {
        Result<UpWheres<WhereJoin>> adjUpWheres = new Result<>(upWheres);
        return getWhereJoins(pushJoin, isInner, adjUpWheres).getCostPushWhere(pushJoin, pushLargeDepth, adjUpWheres.result, keyStat, StatType.PUSH_OUTER(), pushJoinWhere, debugInfoWriter);
    }

    private <K extends Expr> WhereJoins getWhereJoins(QueryJoin<K, ?, ?, ?> pushJoin, boolean isInner, Result<UpWheres<WhereJoin>> upWheres) {
        if(isInner) {
            if(pushJoin.isValue()) { // проблема что queryJoin может быть в ExprStatJoin.valueJoins, тогда он будет Inner, а в WhereJoins его не будет и начнут падать assertion'ы появлятся висячие ключи, другое дело, что потом надо убрать в EqualsWhere ExprStatJoin = значение, тогда это проверка не нужно
                upWheres.set(UpWheres.EMPTY());
                return WhereJoins.EMPTY;
            }
        }

        WhereJoins adjWhereJoins = this;
        // recursion guard, иначе бесконечные проталкивания могут быть (проблема с проталкиванием скажем SubQuery.v=5 должна в принципе решаться другим механищзмом)
        // в том числе и left приходится проверять так как может проталкиваться скажем UnionJoin, а он дает left join'ы 
        WhereJoins removedWhereJoins = removeJoin(pushJoin, upWheres.result, upWheres);
        if(removedWhereJoins != null)
            adjWhereJoins = removedWhereJoins;
        else
            assert !isInner;
        
        return adjWhereJoins;
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
        for(WhereJoin where : wheres)
            result.exclAdd(where, up.get(where).or(getMeanUpWheres(where, meanWheres, upMeans)));
        return new UpWheres<>(result.immutable());
    }

    public static UpWhere getMeanUpWheres(WhereJoin where, WhereJoins meanWheres, UpWheres<WhereJoin> upMeans) {
        UpWhere up2Where = upMeans.get(where);
        if(up2Where==null) { // то есть значит в следствии
            InnerExpr followExpr;
            for(WhereJoin up2Join : meanWheres.wheres)
                if((followExpr=((InnerJoin)where).getInnerExpr(up2Join))!=null) {
                    up2Where = followExpr.getUpNotNullWhere();
                    break;
                }
        }
        return up2Where;
    }

    // limitHeur, indexNotNulls
    public void fillCompileInfo(Result<Cost> mBaseCost, Result<Stat> mRows, Result<ImSet<BaseExpr>> usedNotNulls, Result<ImOrderSet<BaseJoin>> joinOrder, Result<Boolean> mOptAdjustLimit, ImSet<KeyExpr> keys, KeyStat keyStat, LimitOptions limit, ImOrderSet<Expr> orders, DebugInfoWriter debugInfoWriter) {
        Result<CompileInfo> compileInfo = new Result<>(); 
        StatType statType = StatType.COMPILE;
        StatKeys<KeyExpr> statKeys = getStatKeys(keys, null, keyStat, statType, compileInfo, debugInfoWriter);// newNotNull
        usedNotNulls.set(compileInfo.result.usedNotNulls);
        joinOrder.set(compileInfo.result.joinOrder);

        Cost baseCost = statKeys.getCost();
        Stat stat = statKeys.getRows();

        // хитрая эвристика - если есть limit и он маленький, докидываем маленькую статистику по порядкам
        // фактически если есть хороший план с поиском первых записей, то логично что и фильтрация будет быстрой (обратное впрочем не верно, но в этом и есть эвристика
        // !!! ВАЖНА для GROUP LAST / ANY оптимизации (isLastOpt)
        if(limit.hasLimit() && !Settings.get().isDisableAdjustLimitHeur() && Stat.ONE.less(baseCost.rows)) {
            WhereJoins whereJoins = this;

            Cost limitCost = baseCost;
            Stat limitStat = stat;
            // следующая часть эвристично определяет наличие индексов по порядкам
            int i=0,size=orders.size();
            for(;i<size;i++) {
                Expr order = orders.get(i);
                if(order instanceof BaseExpr) {
                    whereJoins = whereJoins.and(new WhereJoins(new ExprStatJoin((BaseExpr)order, Stat.ONE)));
                    statKeys = whereJoins.getStatKeys(keys, null, keyStat, statType, null, debugInfoWriter != null ? debugInfoWriter.pushPrefix("LIMIT ORDER " + i + " of "  + size +" - " + order) : null);

                    Cost newBaseCost = statKeys.getCost();
                    Stat newStat = statKeys.getRows();
                    Stat costReduce = limitCost.rows.div(newBaseCost.rows);
                    Stat statReduce = limitStat.div(newStat);
                    // если cost упал на столько же сколько и stat значит явно есть индекс
                    if(statReduce.lessEquals(costReduce)) { // по идее equals, но на всякий случай
                        limitCost = newBaseCost;
                        limitStat = newStat;
                        continue;
                    }
                }
                break;
            }
            if(i>=size) { // если не осталось порядков, значит все просматривать не надо
                // с другой стороны просто деление слишком оптимистичная операция, так как искомые записи могут быть в конце порядка (а он никак в статистике не учитывается)
                Cost newBaseCost = baseCost.div(stat).or(compileInfo.result.maxSubQueryCost); // limitCost.div(limitStat) всегда заведомо меньше baseCost.div(stat)

                boolean optAdjLimit = false;
                if(newBaseCost.less(baseCost.div(new Stat(Settings.get().getUsePessQueryHeurWhenReducedMore())))) // поэтому если уменьшаем на "слишком" много включаем пессимистичный режим
                    optAdjLimit = true;
                if(mOptAdjustLimit.result != null)
                    optAdjLimit = optAdjLimit || mOptAdjustLimit.result;
                mOptAdjustLimit.set(optAdjLimit);

                baseCost = newBaseCost;
            } // else // тут теоретически можно было бы рассмотреть вариант с индексами по части порядков, если предположить, что СУБД может пробежать по индексу первых порядков, а sort сделать "внутри" по последних порядков, но такие планы СУБД вроде строить не умеют   
              // baseCost = limitCost.or(newBaseCost);
            stat = Stat.ONE; // предполагаем что limit отбирает мало записей
        }
            
        if(mBaseCost.result != null)
            baseCost = baseCost.or(mBaseCost.result);
        mBaseCost.set(baseCost);        
        if(mRows.result != null)
            stat = stat.or(mRows.result);
        mRows.set(stat);
    }

    public ImSet<ParamExpr> getOuterKeys() {
        return AbstractOuterContext.getOuterKeys(this);
    }

    public ImSet<Value> getOuterValues() {
        return AbstractOuterContext.getOuterValues(this);
    }

    public boolean enumerate(ContextEnumerator enumerator) {
        return AbstractOuterContext.enumerate(this, enumerator);
    }

    public long getComplexity(boolean outer) {
        return AbstractOuterContext.getComplexity(this, outer);
    }

    public WhereJoins pack() {
        throw new RuntimeException("not supported yet");
    }

    public ImSet<StaticValueExpr> getOuterStaticValues() {
        throw new RuntimeException("should not be");
    }
}
