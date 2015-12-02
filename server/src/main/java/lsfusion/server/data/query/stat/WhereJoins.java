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
import lsfusion.server.data.query.innerjoins.KeyEqual;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.where.DNFWheres;
import lsfusion.server.data.where.Where;
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

    private final static LRUWVSMap<WhereJoin, ImSet<WhereJoin>> cacheAllChildren = new LRUWVSMap<WhereJoin, ImSet<WhereJoin>>(LRUUtil.L1);

    public static ImSet<WhereJoin> getAllChildren(WhereJoin where) {
        ImSet<WhereJoin> result = cacheAllChildren.get(where);
        if(result == null) {
            result = BaseUtils.getAllChildren(where, getJoins);
            cacheAllChildren.put(where, result);
        }
        return result;
    }

    private final static LRUWSSVSMap<WhereJoins, ImSet, KeyStat, StatKeys> cacheCompileStatKeys = new LRUWSSVSMap<WhereJoins, ImSet, KeyStat, StatKeys>(LRUUtil.L1);
    // можно было бы локальный кэш как и сверху сделать, но также как и для children будет сильно мусорить в алгоритме
    public <K extends BaseExpr> StatKeys<K> getCompileStatKeys(ImSet<K> groups, KeyStat keyStat) {
        StatKeys result = cacheCompileStatKeys.get(this, groups, keyStat);
        if(result==null) {
            result = getStatKeys(groups, keyStat);
            cacheCompileStatKeys.put(this, groups, keyStat, result);
        }
        return result;
    }


    private static BaseUtils.ExChildrenInterface<WhereJoin> getJoins = new BaseUtils.ExChildrenInterface<WhereJoin>() {
        public Iterable<WhereJoin> getChildrenIt(WhereJoin element) {
            return BaseUtils.immutableCast(element.getJoinFollows(new Result<ImMap<InnerJoin, Where>>(), null).it());
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

    private static class Edge<K> {
        public BaseJoin<K> join;
        public Stat keyStat;
        public BaseExpr expr;

        public Stat getKeyStat(MAddMap<BaseJoin, Stat> statJoins) {
            return keyStat.min(statJoins.get(join));
        }
        public Stat getPropStat(MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, Stat> propStats) {
            return WhereJoins.getPropStat(expr, joinStats, propStats);
        }

        private Edge(BaseJoin<K> join, Stat keyStat, BaseExpr expr) {
            this.join = join;
            this.keyStat = keyStat;
            this.expr = expr;
        }
    }

    private static Stat getPropStat(BaseExpr valueExpr, MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, Stat> propStats) {
        return propStats.get(valueExpr).min(joinStats.get(valueExpr.getBaseJoin()));
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, KeyStat keyStat) {
        return getStatKeys(groups, null, keyStat);        
    }

    private final static SimpleAddValue<BaseJoin, Stat> minStat = new SymmAddValue<BaseJoin, Stat>() {
        public Stat addValue(BaseJoin key, Stat prevValue, Stat newValue) {
            return prevValue.min(newValue);
        }
    };

    public static <K> ImMap<K, BaseExpr> getJoinsForStat(BaseJoin<K> join) { // нужно чтобы не терялись ключи у Union в статистике, всегда добавлять их нельзя так как начнет следствия notNull рушить (для NotNullParams)
        if(join instanceof UnionJoin)
            return (ImMap<K, BaseExpr>) ((UnionJoin) join).getJoins(true);
        return join.getJoins();
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, Result<Stat> rows, final KeyStat keyStat) {
        return getStatKeys(groups, rows, keyStat, null, null, null);
    }

    // assert что rows >= result
    // можно rows в StatKeys было закинуть как и ExecCost, но используется только в одном месте и могут быть проблемы с кэшированием
    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, Result<Stat> rows, final KeyStat keyStat, Result<BaseExpr> newNotNull, MAddMap<BaseExpr, Boolean> proceededNotNulls, Result<ExecCost> tableCosts) {

        // groups учавствует только в дополнительном фильтре
        final MAddMap<BaseJoin, Stat> joinStats = MapFact.mAddOverrideMap();
        final MAddMap<BaseExpr, Stat> exprStats = MapFact.mAddOverrideMap();

        final MAddMap<Table.Join, Stat> indexedStats = tableCosts != null ? MapFact.<Table.Join, Stat>mAddOverrideMap() : null;

        MAddExclMap<BaseExpr, Set<Edge>> balancedEdges = MapFact.mAddExclMap(); // assert edge.expr == key
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

        if(tableCosts != null) {
            Stat tableStat = Stat.ONE;
            for(int i=0,size=indexedStats.size();i<size;i++)
                tableStat = tableStat.or(indexedStats.getValue(i));
            tableCosts.set(new ExecCost(tableStat));
        }

        DistinctKeys<K> distinct = new DistinctKeys<K>(groups.mapValues(new GetValue<Stat, K>() {
            public Stat getMapValue(K value) { // для groups, берем min(из статистики значения, статистики его join'а)
                return getPropStat(value, joinStats, exprStats).min(finalStat);
            }}));
        return StatKeys.create(finalStat, distinct); // возвращаем min(суммы groups, расчитанного результата)
    }

    private <K extends BaseExpr> void buildBalancedGraph(ImSet<K> groups, KeyStat keyStat, MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, Stat> exprStats, MAddExclMap<BaseExpr, Set<Edge>> balancedEdges, MAddExclMap<BaseExpr, Stat> balancedStats, Result<BaseExpr> newNotNull, MAddMap<BaseExpr, Boolean> proceededNotNulls, MAddMap<Table.Join, Stat> indexedStats) {
        Set<Edge> edges = SetFact.mAddRemoveSet();
        
        buildGraph(groups, keyStat, exprStats, joinStats, edges, proceededNotNulls, newNotNull);

        balanceGraph(joinStats, exprStats, edges, balancedEdges, balancedStats, indexedStats);
    }

    // balancedEdges - исходящие edges для всех "внутренних" expr, название конечно не совсем корректное
    // balancedStats - уже скорректированная статистика, только для "внутренних" expr, не включая groups, в принципе можно совместить с exprStats, пробежав по groups и хакинув туда getPropStat(value, joinStats, exprStats), но пока особого смысла нет
    private void balanceGraph(MAddMap<BaseJoin, Stat> joinStats, MAddMap<BaseExpr, Stat> exprStats, Set<Edge> unbalancedEdges, MAddExclMap<BaseExpr, Set<Edge>> balancedEdges, MAddExclMap<BaseExpr, Stat> balancedStats, MAddMap<Table.Join, Stat> indexedStats) {
        // ищем несбалансированное ребро с минимальной статистикой
        Stat currentStat = null;
        MAddExclMap<BaseExpr, Set<Edge>> currentBalancedEdges = MapFact.mAddExclMap();

        if(indexedStats != null)
            for(int i=0,size=joinStats.size();i<size;i++) {
                BaseJoin join = joinStats.getKey(i);
                if(join instanceof Table.Join)
                    indexedStats.add((Table.Join)join, joinStats.getValue(i));
            }

        while(unbalancedEdges.size() > 0 || currentBalancedEdges.size() > 0) {
            Edge<?> unbalancedEdge = null;
            Pair<Stat, Stat> unbalancedStat = null;

            Stat stat = Stat.MAX;
            for(Edge edge : unbalancedEdges) {
                Stat keys = edge.getKeyStat(joinStats);
                Stat values = edge.getPropStat(joinStats, exprStats);
                Stat min = keys.min(values);
                if(min.less(stat)) { // если нашли новый минимум про старый забываем
                    unbalancedEdge = edge;
                    unbalancedStat = new Pair<Stat, Stat>(keys, values);
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
                        Set<Edge> exprEdges = currentBalancedEdges.getValue(i);

                        for(int j=0;j<balancedEdges.size();j++) { // бежим по всем уже сбалансированным и пытаемся поддержать cross-column статистику
                            BaseExpr bExpr = balancedEdges.getKey(j);
                            Set<Edge> bExprEdges = balancedEdges.getValue(j);
                            Stat bStat = balancedStats.get(bExpr);

                            List<Pair<Edge, Edge>> mergeEdges = new ArrayList<Pair<Edge, Edge>>();
                            Iterator<Edge> it = exprEdges.iterator();
                            while(it.hasNext()) {
                                Edge exprEdge = it.next();

                                Edge bExprEdge = null;
                                boolean found = false;
                                Iterator<Edge> bit = bExprEdges.iterator();
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
                                    mergeEdges.add(new Pair<Edge, Edge>(exprEdge, bExprEdge));
                                }
                            }

                            if(mergeEdges.size() > 1) { // если пара используется несколько раз объединим
                                ConcatenateExpr concExpr = new ConcatenateExpr(ListFact.toList(expr, bExpr)); // создаем общую вершину
                                InnerBaseJoin<?> concJoin = concExpr.getBaseJoin();
                                Stat mergedStat = currentStat.mult(bStat);
//                                balanced = balanced.mult(mergedStat); // добавляем два внутренних edge'а (обработанных), собсно так как они потом не будут использовать просто добавим в статистику
                                exprEdges.add(new Edge(concJoin, currentStat, expr));
                                bExprEdges.add(new Edge(concJoin, bStat, bExpr));
                                joinStats.add(concJoin, mergedStat); exprStats.add(concExpr, mergedStat); // добавляем join \ записываем статистику
                                for(Pair<Edge, Edge> mergeEdge : mergeEdges) { // добавляем внешние (возможно не сбалансированные edge'и)
                                    assert BaseUtils.hashEquals(mergeEdge.first.join, mergeEdge.second.join);
                                    Edge mergedEdge = new Edge(mergeEdge.first.join, mergedStat, concExpr);
                                    unbalancedEdges.add(mergedEdge);
                                }
                                unbalancedEdge = null; // сбрасываем текущую итерацию и начинаем заново
                            } else {
                                if(mergeEdges.size()==1) { // вернем на место
                                    Pair<Edge, Edge> single = BaseUtils.single(mergeEdges);
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

                Set<Edge> exprEdges = currentBalancedEdges.get(unbalancedEdge.expr);
                if(exprEdges==null) {
                    exprEdges = SetFact.mAddRemoveSet();
                    currentBalancedEdges.exclAdd(unbalancedEdge.expr, exprEdges);
                }
                exprEdges.add(unbalancedEdge);
            }
        }
    }

    private <K extends BaseExpr> void buildGraph(ImSet<K> groups, KeyStat keyStat, MAddMap<BaseExpr, Stat> exprStats, MAddMap<BaseJoin, Stat> joinStats, Set<Edge> edges, MAddMap<BaseExpr, Boolean> proceededNotNulls, Result<BaseExpr> newNotNull) {
        Set<BaseExpr> exprs = SetFact.mAddRemoveSet();
        Set<BaseJoin> joins = SetFact.mAddRemoveSet();

        buildEdgesExprsJoins(groups, keyStat, edges, exprs, joins);

        for(BaseJoin join : joins)
            joinStats.add(join, join.getStatKeys(keyStat).rows);

        int intStat = Settings.get().getAverageIntervalStat();
        if(intStat >= 0)
            for(ExprIndexedJoin join : ExprIndexedJoin.getIntervals(wheres))
                joinStats.add(join, new Stat(intStat, true));

        // читаем статистику по значениям
        for(BaseExpr expr : exprs) {
            PropStat exprStat = expr.getStatValue(keyStat);
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

    private <K extends BaseExpr> void buildEdgesExprsJoins(ImSet<K> groups, KeyStat keyStat, Set<Edge> edges, Set<BaseExpr> exprs, Set<BaseJoin> joins) {
        // собираем все ребра и вершины
        Queue<BaseJoin> queue = new LinkedList<BaseJoin>();
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

                edges.add(new Edge(join, join.getStatKeys(keyStat).distinct.get(joinKey), joinExpr));

                exprs.add(joinExpr);
                InnerBaseJoin<?> valueJoin = joinExpr.getBaseJoin();
                if(!joins.contains(valueJoin)) {
                    queue.add(valueJoin);
                    joins.add(valueJoin);
                }
            }
        }
    }

    private Stat getEdgeRowStat(MAddMap<BaseJoin, Stat> joinStats, MAddExclMap<BaseExpr, Set<Edge>> balancedEdges, MAddExclMap<BaseExpr, Stat> balancedStats) {
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

    private Stat getMSTCost(MAddMap<BaseJoin, Stat> joinStats, MAddExclMap<BaseExpr, Set<Edge>> balancedEdges, MAddExclMap<BaseExpr, Stat> balancedStats) {
        UndirectedGraph<BaseJoin> graph = new UndirectedGraph<BaseJoin>();
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

    private Stat getMSTExCost(MAddMap<BaseJoin, Stat> joinStats, MAddExclMap<BaseExpr, Set<Edge>> balancedEdges, MAddExclMap<BaseExpr, Stat> balancedStats) {
        int nodes = 0; int edges = 0;

        SpanningTreeWithBlackjack<BaseJoin> graph = new SpanningTreeWithBlackjack<BaseJoin>();
        for(int i=0,size=joinStats.size();i<size;i++) {
            BaseJoin node = joinStats.getKey(i);
            graph.addNode(node, node.getJoins().isEmpty() ? 0 : joinStats.getValue(i).getWeight());
            nodes++;
        }

        for(int i=0;i<balancedEdges.size();i++) {
            BaseExpr bExpr = balancedEdges.getKey(i);
            BaseJoin from = bExpr.getBaseJoin();
            for(Edge edge : balancedEdges.getValue(i)) {
                assert BaseUtils.hashEquals(edge.expr, bExpr);
                graph.addEdge(from, edge.join, balancedStats.get(bExpr).getWeight());
                edges++;
            }
        }

        int maxIterations = Settings.get().getMaxEdgeIterations();
        return new Stat(graph.calculate(BaseUtils.max(edges - nodes, 1) * maxIterations), true);
    }

    private Stat getMTCost(MAddMap<BaseJoin, Stat> joinStats, MAddExclMap<BaseExpr, Set<Edge>> balancedEdges, MAddExclMap<BaseExpr, Stat> balancedStats, Stat totalBalanced) {
        MExclMap<BaseJoin, MExclSet<Edge>> mEdges = MapFact.mExclMap();
        for(int i=0,size=joinStats.size();i<size;i++) {
            mEdges.exclAdd(joinStats.getKey(i), SetFact.<Edge>mExclSet());
        }
        for(int i=0;i<balancedEdges.size();i++) {
            BaseExpr bExpr = balancedEdges.getKey(i);
            BaseJoin from = bExpr.getBaseJoin();
            MExclSet<Edge> mFromEdges = mEdges.get(from);
            for(Edge edge : balancedEdges.getValue(i)) {
                assert BaseUtils.hashEquals(edge.expr, bExpr);
                mFromEdges.exclAdd(edge);
            }            
        }
        ImMap<BaseJoin, ImSet<Edge>> edges = MapFact.immutable(mEdges);
        Pair<Integer, ImSet<Edge>> mt = recBuildMT(MapFact.buildGraphOrder(edges, new GetValue<BaseJoin, Edge>() {
            public BaseJoin getMapValue(Edge value) {
                return value.join;
            }}), edges, balancedStats, new HashSet<Edge>(), 0, new HashMap<BaseJoin, ImMap<BaseJoin, Edge>>(), 0, null);
        if(mt == null)
            return totalBalanced;
        return totalBalanced.div(new Stat(mt.first, true));
    }
    
    // proceeded - из какой вершины в какую можно пройти и вершина через которую надо идти
    private Pair<Integer, ImSet<Edge>> recBuildMT(ImOrderSet<BaseJoin> order, ImMap<BaseJoin, ImSet<Edge>> edgesOuts, final MAddExclMap<BaseExpr, Stat> balancedStats, Set<Edge> removedEdges, int removedStat, Map<BaseJoin, ImMap<BaseJoin, Edge>> currentTree, int currentIndex, Pair<Integer, ImSet<Edge>> currentMin) {
        if(currentIndex >= order.size()) {
            return new Pair<Integer, ImSet<Edge>>(removedStat, SetFact.fromJavaSet(removedEdges));
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
                            return Integer.compare(balancedStats.get(o1.expr).getWeight(), balancedStats.get(o2.expr).getWeight());
                        }});
                    for(Edge currentEdge : edges) {
                        // пробуем удалить ребро
                        int newRemovedStat = removedStat + balancedStats.get(currentEdge.expr).getWeight();
                        if(currentMin == null || currentMin.first > newRemovedStat) {
                            MAddExclMap<BaseJoin, ImMap<BaseJoin, Edge>> stackRemoved = removeEdge(currentTree, currentEdge);
                            removedEdges.add(currentEdge);
                            Pair<Integer, ImSet<Edge>> recCut = recBuildMT(order, edgesOuts, balancedStats, removedEdges, newRemovedStat, currentTree, currentIndex, currentMin);// придется начинать с 0 чтобы перестроить дерево
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
        Pair<Integer, ImSet<Edge>> result = recBuildMT(order, edgesOuts, balancedStats, removedEdges, removedStat, currentTree, currentIndex + 1, currentMin);
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
        private final MAddMap<WhereJoin, Where> upWheres;

        public PushResult(Stat runStat, List<WhereJoin> joins, MAddMap<WhereJoin, Where> upWheres) {
            this.runStat = runStat;

            this.joins = joins;
            this.upWheres = upWheres;
        }

        private <T extends Expr> Where getWhere(ImMap<T, ? extends Expr> translate) {
            Where result = Where.TRUE;
            for (WhereJoin where : joins)
                result = result.and(upWheres.get(where)).and(BaseExpr.getOrWhere(where)); // чтобы не потерять or, правда при этом removeJoin должен "соответствовать" не TRUE calculateOrWhere

            return GroupExpr.create(translate, result, translate.keys().toMap()).getWhere();
        }
    }

    private <K extends BaseExpr> PushResult getPushJoins(ImSet<K> groups, ImMap<WhereJoin, Where> upWheres, final KeyStat stat, Stat currentStat, Stat currentJoinStat) {
        Comparator<WhereJoin> orderComplexity = new Comparator<WhereJoin>() {
            public int compare(WhereJoin o1, WhereJoin o2) {
                long comp1 = o1.getComplexity(false);
                long comp2 = o2.getComplexity(false);
                if(comp1 < comp2)
                        return 1;
                if(comp1 > comp2)
                        return -1;

                Stat r1 = o1.getStatKeys(stat).rows;
                Stat r2 = o2.getStatKeys(stat).rows;
                if(r1.less(r2))
                    return 1;
                if(r2.less(r1))
                    return -1;

                return 0;
            }};

        Stat baseJoinStat = currentJoinStat;
        Stat baseRowStat = currentStat;

        List<WhereJoin> current;
        WhereJoin[] cloned = wheres.clone();
        Arrays.sort(cloned, orderComplexity);
        current = BaseUtils.toList(cloned);

        Result<Stat> rows = new Result<Stat>();
        Stat resultStat = getStatKeys(groups, rows, stat).rows;
        if(resultStat.lessEquals(currentJoinStat) && rows.result.lessEquals(currentStat)) {
            currentJoinStat = resultStat; currentStat = rows.result;
        }

        MAddMap<WhereJoin, Where> reducedUpWheres = MapFact.mAddOverrideMap(upWheres); // может проходить несколько раз по одной ветке
        int it = 0;
        while(it < current.size()) {
            WhereJoin<?, ?> reduceJoin = current.get(it);

            List<WhereJoin> reduced = new ArrayList<WhereJoin>(current);
            reduced.remove(it);

            Result<ImMap<InnerJoin, Where>> reduceFollowUpWheres = new Result<ImMap<InnerJoin, Where>>();
            for(InnerJoin joinFollow : reduceJoin.getJoinFollows(reduceFollowUpWheres, null).it()) { // пытаемся заменить reduceJoin, на его joinFollows
                boolean found = false;
                for(WhereJoin andJoin : reduced)
                    if(containsAll(andJoin, joinFollow)) {
                        found = true;
                        break;
                    }
                if(!found) {
                    BaseUtils.addToOrderedList(reduced, joinFollow, it, orderComplexity);
                    reducedUpWheres.add(joinFollow, reduceFollowUpWheres.result.get(joinFollow));
                }
            }

            WhereJoins reducedJoins = new WhereJoins(reduced.toArray(new WhereJoin[reduced.size()]));
            rows = new Result<Stat>();
            Stat reducedStat = reducedJoins.getStatKeys(groups, rows, stat).rows;
//            assert !reducedJoins.getStatKeys(groups, stat).rows.less(resultStat.rows); // вообще это не правильный assertion, потому как если уходит ключ статистика может уменьшиться
            if(reducedStat.lessEquals(currentJoinStat) && rows.result.lessEquals(currentStat)) { // сколько сгруппировать надо
                currentJoinStat = reducedStat; currentStat = rows.result; current = reduced;
            } else
                it++;
        }

        return new PushResult(baseRowStat.mult(currentJoinStat).div(baseJoinStat).or(currentStat), current, reducedUpWheres);
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, final KeyStat keyStat, final KeyEqual keyEqual) {
        if(!keyEqual.isEmpty()) { // для оптимизации
            return and(keyEqual.getWhereJoins()).getStatKeys(groups, keyEqual.getKeyStat(keyStat));
        } else
            return getStatKeys(groups, keyStat);
    }

    public static <T extends WhereJoin> WhereJoins removeJoin(QueryJoin removeJoin, WhereJoin[] wheres, ImMap<WhereJoin, Where> upWheres, Result<ImMap<WhereJoin, Where>> resultWheres) {
        WhereJoins result = null;
        ImMap<WhereJoin, Where> resultUpWheres = null;
        MExclSet<WhereJoin> mKeepWheres = SetFact.mExclSetMax(wheres.length); // массивы
        for(WhereJoin whereJoin : wheres) {
            WhereJoins removeJoins;
            Result<ImMap<WhereJoin, Where>> removeUpWheres = new Result<ImMap<WhereJoin, Where>>();

            boolean remove = BaseUtils.hashEquals(removeJoin, whereJoin);
            InnerJoins joinFollows = null; Result<ImMap<InnerJoin, Where>> joinUpWheres = null;
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
                Result<ImSet<UnionJoin>> unionJoins = new Result<ImSet<UnionJoin>>();
                joinUpWheres = new Result<ImMap<InnerJoin, Where>>();
                joinFollows = whereJoin.getJoinFollows(joinUpWheres, unionJoins);
                for(UnionJoin unionJoin : unionJoins.result) // без этой проверки может бесконечно проталкивать
                    if(unionJoin.depends(removeJoin)) {
                        remove = true;
                        break;
                    }
            }

            if(remove) {
                removeJoins = WhereJoins.EMPTY;
                removeUpWheres.set(MapFact.<WhereJoin, Where>EMPTY());
            } else
                removeJoins = joinFollows.removeJoin(removeJoin,
                        BaseUtils.<ImMap<WhereJoin,Where>>immutableCast(joinUpWheres.result), removeUpWheres);

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
            resultWheres.set(result.andUpWheres(resultUpWheres, upWheres.filterIncl(keepWheres)));
            return result;
        }
        return null;
    }

    // устраняет сам join чтобы при проталкивании не было рекурсии
    public WhereJoins removeJoin(QueryJoin join, ImMap<WhereJoin, Where> upWheres, Result<ImMap<WhereJoin, Where>> resultWheres) {
        return removeJoin(join, wheres, upWheres, resultWheres);
    }

    public <K extends Expr> Where getGroupPushWhere(final ImMap<K, BaseExpr> joinMap, ImMap<WhereJoin, Where> upWheres, QueryJoin<K, ?, ?, ?> skipJoin, KeyStat keyStat, Where fullWhere, StatKeys<K> currentJoinStat) {
        return getPushWhere(joinMap, upWheres, skipJoin, keyStat, fullWhere, currentJoinStat, null);
    }

    // получает подможнство join'ов которое дает joinKeys, пропуская skipJoin. тут же алгоритм по определению достаточных ключей
    // !!! ТЕОРЕТИЧЕСКИ НЕСМОТРЯ НА REMOVE из-за паковки может проталкивать бесконечно (впоследствии нужен будет GUARD), например X = L(G1 + G2) AND (G1 OR G2) спакуется в X = L(G1 + G2) AND (G1' OR G2) , (а не L(G1' + G2), и будет G1 проталкивать бесконечно)
    //  но это очень редкая ситуация и важно проследить за ее природой, так как возможно есть аналогичные assertion'ы
    // может неправильно проталкивать в случае если скажем есть документы \ строки, строки "материализуются" и если они опять будут группироваться по документу, информация о том что он один уже потеряется
    private <K extends Expr, T extends Expr> Where getPushWhere(ImMap<K, BaseExpr> joinMap, ImMap<WhereJoin, Where> upWheres, QueryJoin<K, ?, ?, ?> skipJoin, KeyStat keyStat, Where fullWhere, StatKeys<K> currentJoinStat, Provider<ImMap<T, ? extends Expr>> getTranslate) {
        // joinKeys из skipJoin.getJoins()

        assert joinMap.equals(skipJoin.getJoins().filterIncl(joinMap.keys()));
        Result<ImMap<WhereJoin, Where>> upFitWheres = new Result<ImMap<WhereJoin, Where>>();
        WhereJoins removedJoins = removeJoin(skipJoin, upWheres, upFitWheres);
        if(removedJoins==null) {
            removedJoins = this;
            upFitWheres.set(upWheres);
        }

        return removedJoins.getPushWhere(joinMap, keyStat, fullWhere.getStatRows(), currentJoinStat, upFitWheres.result, getTranslate);
    }

    private static class PushJoinResult<K extends Expr> {
        private final PushResult joins;
        private final ImMap<K, BaseExpr> group;

        public PushJoinResult(PushResult joins, ImMap<K, BaseExpr> group) {
            this.joins = joins;
            this.group = group;
        }
    }

    private <K extends Expr, T extends Expr> Where getPushWhere(ImMap<K, BaseExpr> joinMap, KeyStat keyStat, Stat currentStat, StatKeys<K> currentJoinStat, ImMap<WhereJoin, Where> upWheres, Provider<ImMap<T, ? extends Expr>> getTranslate) {
        assertJoinRowStat(currentStat, currentJoinStat);

        final PushJoinResult<K> pushResult = getPushJoins(joinMap, keyStat, currentStat, currentJoinStat, upWheres, getTranslate, Settings.get().isUseOldPushJoins());
        if(pushResult == null)
            return null;
        return pushResult.joins.getWhere(getTranslate == null ? BaseUtils.<ImMap<T, Expr>>immutableCast(pushResult.group) : getTranslate.get());

//        PushJoinResult<K> oldPushResult = getPushJoins(joinMap, keyStat, currentStat, currentJoinStat, upWheres, getTranslate, true);
//        PushJoinResult<K> pushResult = getPushJoins(joinMap, keyStat, currentStat, currentJoinStat, upWheres, getTranslate, false);
//        if (pushResult == null) {
//            if(oldPushResult != null)
//                pushResult = pushResult;
//            return null;
//        }
//
//        Where where = pushResult.joins.getWhere(getTranslate == null ? BaseUtils.<ImMap<T, Expr>>immutableCast(pushResult.group) : getTranslate.get());
//
//        if(oldPushResult == null)
//            pushResult = pushResult;
//        else {
//            if(!pushResult.joins.runStat.equals(oldPushResult.joins.runStat))
//                pushResult = pushResult;
//            if(!new HashSet<WhereJoin>(pushResult.joins.joins).equals(new HashSet<WhereJoin>(oldPushResult.joins.joins)))
//                pushResult = pushResult;
//            if(!pushResult.joins.upWheres.equals(oldPushResult.joins.upWheres))
//                pushResult = pushResult;
//
//            Where oldWhere = oldPushResult.joins.getWhere(getTranslate == null ? BaseUtils.<ImMap<T, Expr>>immutableCast(oldPushResult.group) : getTranslate.get());
//
//            if(!BaseUtils.hashEquals(where, oldWhere))
//                where = where;
//        }
//
//        return where;
    }

    private <K extends Expr, T extends Expr> PushJoinResult<K> getPushJoins(ImMap<K, BaseExpr> joinMap, KeyStat keyStat, Stat currentStat, StatKeys<K> currentJoinStat, ImMap<WhereJoin, Where> upWheres, Provider<ImMap<T, ? extends Expr>> getTranslate, boolean old) {
        PushJoinResult<K> pushResult;
        Stat baseStat = currentStat.min(Stat.ALOT);
        boolean checkSubsets = getTranslate == null;
        if(old) {
            pushResult = getOldPushJoins(joinMap, keyStat, currentStat, currentJoinStat, upWheres, checkSubsets);
            if(baseStat.lessEquals(pushResult.joins.runStat)) // не уменьшили статистику
                return null;
            if(pushResult.group.isEmpty())
                return null;
        } else {
            pushResult = getNewPushJoins(joinMap, keyStat, currentStat, currentJoinStat, upWheres, baseStat);
            if(pushResult == null)
                return null;
        }
        return pushResult;
    }

    // как правило работает, но в каких то очень редких случаях вроде синхронизации нет, надо будет потом разобраться, пока не критично
    private <K extends Expr> void assertJoinRowStat(Stat currentStat, StatKeys<K> currentJoinStat) {
//        assert currentJoinStat.rows.equals(StatKeys.create(currentStat, currentJoinStat.distinct).rows);
    }

    private <K extends Expr, T extends Expr> PushJoinResult<K> getOldPushJoins(ImMap<K, BaseExpr> joinMap, KeyStat keyStat, Stat currentStat, StatKeys<K> currentJoinStat, ImMap<WhereJoin, Where> upWheres, boolean checkSubsets) {
        PushJoinResult<K> pushResult = getPushJoins(joinMap, keyStat, currentStat, currentJoinStat.rows, upWheres);

        if(checkSubsets && joinMap.size() > 1) { // значит можно reduce делать,  && !pushResult.runStat.less(currentStat)
            for(int i=0,size= joinMap.size();i<size;i++) {
                K key = joinMap.getKey(i);
                PushJoinResult<K> pushSingleResult = getPushJoins(MapFact.singleton(key, joinMap.getValue(i)), keyStat, currentStat, currentJoinStat.distinct.get(key), upWheres);
                if(pushSingleResult.joins.runStat.lessEquals(pushResult.joins.runStat)) {
                    pushResult = pushSingleResult;
                }
            }
        }
        return pushResult;
    }

    private <K extends Expr> PushJoinResult<K> getPushJoins(ImMap<K, BaseExpr> joinMap, KeyStat keyStat, Stat currentStat, Stat currentJoinStat, ImMap<WhereJoin, Where> upWheres) {
        return new PushJoinResult<K>(getPushJoins(joinMap.values().toSet(), upWheres, keyStat, currentStat, currentJoinStat), joinMap);
    }

    private <K extends Expr, T extends Expr> PushJoinResult<K> getNewPushJoins(ImMap<K, BaseExpr> innerOuter, KeyStat keyStat, Stat innerRows, StatKeys<K> innerKeys, ImMap<WhereJoin, Where> upWheres, Stat baseStat) {
        // преобразуем joins в reverse map, пока делаем просто и выбираем минимум innerKeys (то есть с меньшим числом разновидностей)
        MMap<BaseExpr, K> mRevOuterInner = MapFact.mMap(MapFact.<BaseExpr, K>override());
        for(int i=0,size=innerOuter.size();i<size;i++) {
            K inner = innerOuter.getKey(i);
            BaseExpr outer = innerOuter.getValue(i);
            K revInner = mRevOuterInner.get(outer);
            if(revInner == null || innerKeys.distinct.get(inner).less(innerKeys.distinct.get(revInner)))
                mRevOuterInner.add(outer, inner);
        }
        ImRevMap<K, BaseExpr> revInnerOuter = mRevOuterInner.immutable().toRevExclMap().reverse();
        if(revInnerOuter.size() != innerOuter.size())
            innerKeys = new StatKeys<K>(innerKeys.rows, new DistinctKeys<K>(innerKeys.distinct.filterIncl(revInnerOuter.keys())));

        // считаем начальную итерацию, вырезаем WhereJoins которые "входят" в group
        final ImSet<ParamExpr> keepKeys = SetFact.<ParamExpr>EMPTY();
        Comparator<PushElement> comparator = getComparator(keepKeys);
        final ImSet<PushGroup<K>> groups = revInnerOuter.mapSetValues(new GetKeyValue<PushGroup<K>, K, BaseExpr>() {
            public PushGroup<K> getMapValue(K key, BaseExpr value) {
                return new PushGroup<K>(key, value);
            }});
        List<PushElement> newPriority = new ArrayList<>();
        MExclSet<PushElement> mNewElements = SetFact.<PushElement>mExclSet(groups);
        MExclSet<WhereJoin> mNewJoins = SetFact.mExclSet();
        for(PushGroup<K> group : groups)
            BaseUtils.addToOrderedList(newPriority, group, 0, comparator);
        addJoins(Arrays.asList(wheres), upWheres, comparator, newPriority, groups, mNewElements, mNewJoins, null);
        final PushIteration<K> initialIteration = new PushIteration<K>(mNewElements.immutable(), mNewJoins.immutable(), revInnerOuter, keepKeys, newPriority);

        // перебираем
        Result<BestResult> rBest = new Result<BestResult>(new BaseStat(baseStat));
        recPushJoins(initialIteration, keyStat, innerRows, innerKeys, PushIteration.Reduce.NONE, rBest, false);

        if(rBest.result instanceof BaseStat) // не нашли ничего лучше
            return null;
        final PushIteration<K> best = (PushIteration<K>) rBest.result;

        MAddMap<WhereJoin, Where> bestUpWheres = MapFact.mAddOverrideMap();
        for(PushElement element : best.elements)
            if(element instanceof PushJoin) {
                PushJoin join = (PushJoin)element;
                bestUpWheres.add(join.join, join.upWhere);
            }
        return new PushJoinResult<K>(new PushResult(best.runStat, best.joins.toList().toJavaList(), bestUpWheres), best.innerOuter);
    }

    private static abstract class PushElement {

        protected abstract OuterContext<?> getOuterContext();
        protected abstract BaseJoin<?> getBaseJoin();

        public boolean containsAll(WhereJoin join) {
            return containsJoinAll(getBaseJoin(), join);
        }

        private InnerJoins getJoinFollows(Result<ImMap<InnerJoin,Where>> upWheres) {
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
        private final Where upWhere;

        public PushJoin(WhereJoin join, Where upWhere) {
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

        protected Stat runStat;
        protected abstract long getComplexity();

        // обе должны быть убывающими при вырезании join'ов, без изменения PRIM
        protected boolean primBetter(BestResult iteration) { // если лучше, то при remove'е join'ов не убирая ключи или группы результат не улучшишь
            return runStat.less(iteration.runStat);
        }
        protected boolean secBetter(BestResult iteration) {
            return getComplexity() < iteration.getComplexity();
        }
    }

    private static class BaseStat extends BestResult {

        public BaseStat(Stat baseStat) {
            runStat = baseStat;
        }

        protected long getComplexity() { // мнтересует только если статистика строго меньше
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
                sum = sum.mult(keys.distinct.get(group));
            return sum.min(rows);
        }

        // MAX(Ri * MIN (*(Jo), Ro) / MIN(*(Ji), Ri)  , Ro)
        private Stat calcRunStat(ImRevMap<K, BaseExpr> innerOuter, Stat outerRows, StatKeys<BaseExpr> outerKeys, Stat innerRows, StatKeys<K> innerKeys) {
            return innerRows.mult(calcMultStat(innerOuter.valuesSet(), outerRows, outerKeys)).div(calcMultStat(innerOuter.keys(), innerRows, innerKeys)).max(outerRows);
        }

        private void calcRunStat(Stat innerRows, StatKeys<K> innerKeys, KeyStat keyStat) {
            ImRevMap<K, BaseExpr> innerOuter = getInnerOuter();
            WhereJoins joins = getJoins();

            Result<Stat> rows = new Result<>();
            StatKeys<BaseExpr> statKeys = joins.getStatKeys(innerOuter.valuesSet(), rows, keyStat);

            runStat = calcRunStat(innerOuter, rows.result, statKeys, innerRows, innerKeys);
        }

        private long calcComplexity() {
            long result = 0;
            for(WhereJoin element : joins)
                result += element.getComplexity(false);
            for(BaseExpr expr : innerOuter.valueIt())
                result += expr.getComplexity(false);
            return result;
        }

        protected Long complexity;
        @ManualLazy
        protected long getComplexity() {
            if(complexity == null)
                complexity = calcComplexity();
            return complexity;
        }

        private enum Reduce {
            PRIM, // Stat, group keys + keyExprs
            SEC, // PRIM (>=)= best ищем уменьшение SEC (если конечно не reducePrim)
            NONE
        }

        public Reduce checkBest(Reduce forceReduce, Result<BestResult> bestIteration, Stat innerRows, StatKeys<K> innerKeys, KeyStat keyStat) { // возвращает если заведомо хуже best
            if(forceReduce == Reduce.PRIM) // если REDUCE.PRIM то ничего не проверяем
                return forceReduce;

            if(forceReduce == Reduce.SEC) { // если ждем reduce'а вторичного признака, не считаем runStat до того как проверим вторичный признак (но считать runStat все равно придется, чтобы не увеличить его случайно)
                assert bestIteration.result != null; // так как опция Reduce.SEC может включится только при равенстве Redisce.PRIM
                if(!secBetter(bestIteration.result))
                    return forceReduce;
            }

            // считаем runStat
            calcRunStat(innerRows, innerKeys, keyStat);

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

            Comparator<PushElement> comparator = getComparator(newKeepKeys);
            for(int i=1,size=priority.size();i<size;i++) { // обновляем priority с учетом изменения comparator\а
                final PushElement rest = priority.get(i);
                final ImSet<ParamExpr> restKeys = rest.getKeys();
                if(restKeys.intersect(addKeepKeys)) { // если пересекаются ключи, выкидываем, добавляем еще раз
                    newPriority.remove(rest);
                    BaseUtils.addToOrderedList(newPriority, rest, 0, comparator);
                }

                if(rest instanceof PushJoin && this.keepKeys.containsAll(restKeys)) // оптимизация по comparator'у в priority, если все из keep выходим
                    break;
            }

            return new PushIteration<K>(elements, joins, innerOuter, newKeepKeys, newPriority);
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
            Result<ImMap<InnerJoin, Where>> reduceFollowUpWheres = new Result<>();
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

    public static <WJ extends WhereJoin> void addJoins(Iterable<WJ> joins, ImMap<WJ, Where> upWheres, Comparator<PushElement> comparator, List<PushElement> newPriority, ImSet<? extends PushElement> elements, MExclSet<PushElement> mNewElements, MExclSet<WhereJoin> mNewJoins, Set<ParamExpr> removedKeys) {
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

    private <K extends Expr> void recPushJoins(PushIteration<K> iteration, KeyStat keyStat, Stat innerRows, StatKeys<K> innerKeys, PushIteration.Reduce forceReduce, Result<BestResult> best, boolean upKeep) {

        if(!upKeep) // если сверху не обработали эту итерацию (здесь, а не в вырезании чтобы включить первую итерацию)
            forceReduce = iteration.checkBest(forceReduce, best, innerRows, innerKeys, keyStat);

        if(!iteration.hasElement())
            return;

        // оптимизация
        if(forceReduce == PushIteration.Reduce.PRIM && iteration.hasNoReducePrim())
            return;

        // проверяем оставление
        recPushJoins(iteration.keepElement(), keyStat, innerRows, innerKeys, forceReduce, best, true);

        // проверяем удаление
        Result<Boolean> reducedPrim = new Result<>();
        final PushIteration<K> removeIteration = iteration.removeElement(reducedPrim);

        if(removeIteration.getInnerOuter().isEmpty()) // если группировок не осталось выходим
            return;

        if (reducedPrim.result) // сбрасываем prim, если "ушел" один из значимых признаков (группировка или не keep ключ)
            forceReduce = PushIteration.Reduce.NONE;

        recPushJoins(removeIteration, keyStat, innerRows, innerKeys, forceReduce, best, false);
    }

    public Where getPartitionPushWhere(ImMap<KeyExpr, BaseExpr> joinMap, final ImSet<Expr> partitions, ImMap<WhereJoin, Where> upWheres, QueryJoin<KeyExpr, ?, ?, ?> skipJoin, KeyStat keyStat, Where fullWhere, StatKeys<KeyExpr> currentJoinStat) {
        joinMap = joinMap.filterIncl(BaseUtils.<ImSet<KeyExpr>>immutableCast(AbstractOuterContext.getOuterSetKeys(partitions)));

        final ImMap<KeyExpr, BaseExpr> fJoinMap = joinMap;
        return getPushWhere(joinMap, upWheres, skipJoin, keyStat, fullWhere, currentJoinStat, new Provider<ImMap<Expr, ? extends Expr>>() {
            public ImMap<Expr, ? extends Expr> get() {
                return new QueryTranslator(fJoinMap).translate(partitions.toMap());
            }
        });
    }

    // может как MeanUpWheres сделать
    public static <J extends WhereJoin> ImMap<J, Where> andUpWheres(J[] wheres, ImMap<J, Where> up1, ImMap<J, Where> up2) {
        MExclMap<J, Where> result = MapFact.mExclMap(wheres.length); // массивы
        for(J where : wheres) {
            Where where1 = up1.get(where);
            Where where2 = up2.get(where);
            Where andWhere;
            if(where1==null)
                andWhere = where2;
            else
                if(where2==null)
                    andWhere = where1;
                else
                    andWhere = where1.and(where2);
            result.exclAdd(where, andWhere);
        }
        return result.immutable();
    }

    public ImMap<WhereJoin, Where> andUpWheres(ImMap<WhereJoin, Where> up1, ImMap<WhereJoin, Where> up2) {
        return andUpWheres(wheres, up1, up2);
    }

    public ImMap<WhereJoin, Where> orUpWheres(ImMap<WhereJoin, Where> up1, ImMap<WhereJoin, Where> up2) {
        MExclMap<WhereJoin, Where> result = MapFact.mExclMap(wheres.length); // массивы
        for(WhereJoin where : wheres)
            result.exclAdd(where, up1.get(where).or(up2.get(where)));
        return result.immutable();
    }

    // из upMeans следует
    public ImMap<WhereJoin, Where> orMeanUpWheres(ImMap<WhereJoin, Where> up, WhereJoins meanWheres, ImMap<WhereJoin, Where> upMeans) {
        MExclMap<WhereJoin, Where> result = MapFact.mExclMap(wheres.length); // массивы
        for(WhereJoin where : wheres) {
            Where up2Where = upMeans.get(where);
            if(up2Where==null) { // то есть значит в следствии
                InnerExpr followExpr;
                for(WhereJoin up2Join : meanWheres.wheres)
                    if((followExpr=((InnerJoin)where).getInnerExpr(up2Join))!=null) {
                        up2Where = followExpr.getWhere();
                        break;
                    }
            }
            result.exclAdd(where, up.get(where).or(up2Where));
        }
        return result.immutable();
    }
    
    // вообще при таком подходе, скажем из-за формул в ExprJoin, LEFT JOIN'ы могут быть раньше INNER, но так как SQL Server это позволяет бороться до конца за это не имеет особого смысла 
    public Where fillInnerJoins(ImMap<WhereJoin, Where> upWheres, MList<String> whereSelect, Result<ExecCost> mBaseCost, CompileSource source, ImSet<KeyExpr> keys, KeyStat keyStat) {
        Where innerWhere = Where.TRUE;
        for (WhereJoin where : wheres)
            if(!(where instanceof ExprIndexedJoin && ((ExprIndexedJoin)where).givesNoKeys())) {
                Where upWhere = upWheres.get(where);
                String upSource = upWhere.getSource(source);
                if(where instanceof ExprJoin && ((ExprJoin)where).isClassJoin()) {
                    whereSelect.add(upSource);
                    innerWhere = innerWhere.and(upWhere);
                }
            }

        Result<BaseExpr> newNotNull = new Result<BaseExpr>();
        MAddMap<BaseExpr, Boolean> proceedNotNulls = MapFact.mAddOverrideMap();
        Result<ExecCost> mTableCosts = new Result<ExecCost>();
        Stat baseStat = getStatKeys(keys, null, keyStat, newNotNull, proceedNotNulls, mTableCosts).rows;

        ExecCost baseCost = mTableCosts.result;
        if(mBaseCost.result != null)
            baseCost = baseCost.or(mBaseCost.result);
        mBaseCost.set(baseCost);

        if(source.syntax.hasNotNullIndexProblem()) {
            while(true) {
                BaseExpr notNull = newNotNull.result;
                if(notNull == null)
                    break;

                proceedNotNulls.add(notNull, true); // пробуем без этого notNull
                newNotNull.set(null);
                Stat newStat = getStatKeys(keys, null, keyStat, newNotNull, proceedNotNulls, null).rows;
                if(baseStat.less(newStat)) // если реально использовался помечаем как "важный"
                    proceedNotNulls.add(notNull, false);
            }
            for(int i=0,size=proceedNotNulls.size();i<size;i++) {
                if(!proceedNotNulls.getValue(i)) {
                    whereSelect.add(proceedNotNulls.getKey(i).getSource(source) + " IS NOT NULL");
                }
            }
        }
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
}
