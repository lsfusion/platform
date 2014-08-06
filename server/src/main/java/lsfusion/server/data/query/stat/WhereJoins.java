package lsfusion.server.data.query.stat;

import lsfusion.base.AddSet;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.query.*;
import lsfusion.server.data.query.*;
import lsfusion.server.data.query.innerjoins.KeyEqual;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.where.DNFWheres;
import lsfusion.server.data.where.Where;

import java.util.*;

public class WhereJoins extends AddSet<WhereJoin, WhereJoins> implements DNFWheres.Interface<WhereJoins>, OuterContext<WhereJoins> {

    private WhereJoins() {
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
    // assert что rows >= result
    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, Result<Stat> rows, final KeyStat keyStat) {
        final MAddMap<BaseJoin, Stat> joinStats = MapFact.mAddOverrideMap();
        final MAddMap<BaseExpr, Stat> exprStats = MapFact.mAddOverrideMap();

        Set<Edge> edges = SetFact.mAddRemoveSet();

        // собираем все ребра и вершины
        Set<BaseJoin> joins = SetFact.mAddRemoveSet();
        Set<BaseExpr> exprs = SetFact.mAddRemoveSet();
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

        for(BaseJoin join : joins)
            joinStats.add(join, join.getStatKeys(keyStat).rows);

        // читаем статистику по значениям
        MAddMap<BaseJoin, Stat> exprNotNullStats = MapFact.mAddMap(minStat);
        for(BaseExpr expr : exprs) {
            PropStat exprStat = expr.getStatValue(keyStat);
            exprStats.add(expr, exprStat.distinct);
            if(exprStat.notNull!=null)
                exprNotNullStats.add(expr.getBaseJoin(), exprStat.notNull);
        }
        // уменьшаем статистику join'а до минимального notNull значения
        for(int i=0,size=exprNotNullStats.size();i<size;i++) {
            BaseJoin notNullJoin = exprNotNullStats.getKey(i);
            Stat stat = exprNotNullStats.getValue(i);
//            assert stat.lessEquals(joinStats.get(notNullJoin));
            joinStats.add(notNullJoin, stat);
        }

        MAddExclMap<BaseExpr, Set<Edge>> balancedEdges = MapFact.mAddExclMap();
        MAddExclMap<BaseExpr, Stat> balancedStats = MapFact.mAddExclMap();

        // ищем несбалансированное ребро с минимальной статистикой
        Stat currentStat = null;
        MAddExclMap<BaseExpr, Set<Edge>> currentBalancedEdges = MapFact.mAddExclMap();

        Stat balanced = Stat.ONE;
        while(edges.size() > 0 || currentBalancedEdges.size() > 0) {
            Edge<?> unbalancedEdge = null;
            Pair<Stat, Stat> unbalancedStat = null;
            
            Stat stat = Stat.MAX;
            for(Edge edge : edges) {
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
                                Stat mergedStat = currentStat.mult(bStat);
                                balanced = balanced.mult(mergedStat); // добавляем два внутренних edge'а (обработанных), собсно так как они потом не будут использовать просто добавим в статистику
                                joinStats.add(concExpr.getBaseJoin(), mergedStat); exprStats.add(concExpr, mergedStat); // добавляем join \ записываем статистику
                                for(Pair<Edge, Edge> mergeEdge : mergeEdges) { // добавляем внешние (возможно не сбалансированные edge'и)
                                    assert BaseUtils.hashEquals(mergeEdge.first.join, mergeEdge.second.join);
                                    Edge mergedEdge = new Edge(mergeEdge.first.join, mergedStat, concExpr);
                                    edges.add(mergedEdge);
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
                if(unbalancedStat.first.less(unbalancedStat.second)) { // балансируем значение
                    Stat decrease = unbalancedStat.second.div(unbalancedStat.first);
                    exprStats.add(unbalancedEdge.expr, unbalancedStat.first); // это и есть разница
                    BaseJoin valueJoin = unbalancedEdge.expr.getBaseJoin();
                    joinStats.add(valueJoin, joinStats.get(valueJoin).div(decrease));
                } else { // балансируем ключ, больше он использоваться не будет
                    Stat decrease = unbalancedStat.first.div(unbalancedStat.second);
                    joinStats.add(unbalancedEdge.join, joinStats.get(unbalancedEdge.join).div(decrease));
                }
                edges.remove(unbalancedEdge);
                Set<Edge> exprEdges = currentBalancedEdges.get(unbalancedEdge.expr);
                if(exprEdges==null) {
                    exprEdges = SetFact.mAddRemoveSet();
                    currentBalancedEdges.exclAdd(unbalancedEdge.expr, exprEdges);
                }
                exprEdges.add(unbalancedEdge);
            }
        }

        // бежим по всем сбалансированным ребрам суммируем, бежим по всем нодам суммируем, возвращаем разность
        for(int i=0;i<balancedEdges.size();i++)
            balanced = balanced.mult(balancedStats.get(balancedEdges.getKey(i)).deg(balancedEdges.getValue(i).size()));
        Stat rowStat = Stat.ONE;
        for(int i=0,size=joinStats.size();i<size;i++)
            rowStat = rowStat.mult(joinStats.getValue(i));
        final Stat finalStat = rowStat.div(balanced);
        if(rows!=null)
            rows.set(finalStat);

        DistinctKeys<K> distinct = new DistinctKeys<K>(groups.mapValues(new GetValue<Stat, K>() {
            public Stat getMapValue(K value) { // для groups, берем min(из статистики значения, статистики его join'а)
                return getPropStat(value, joinStats, exprStats).min(finalStat);
            }}));
        return new StatKeys<K>(distinct.getMax().min(finalStat), distinct); // возвращаем min(суммы groups, расчитанного результата)
    }

    public <K extends BaseExpr> Where getPushWhere(ImSet<K> groups, ImMap<WhereJoin, Where> upWheres, final KeyStat stat, Stat currentStat, Stat currentJoinStat) {
        // нужно попытаться опускаться ниже, устраняя "избыточные" WhereJoin'ы или InnerJoin'ы

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

        List<WhereJoin> current;
        WhereJoin[] cloned = wheres.clone();
        Arrays.sort(cloned, orderComplexity);
        current = BaseUtils.toList(cloned);

        Stat startJoinStat = currentJoinStat; 
        
        Result<Stat> rows = new Result<Stat>();
        Stat resultStat = getStatKeys(groups, rows, stat).rows;
        if(resultStat.lessEquals(currentJoinStat) && rows.result.lessEquals(currentStat)) {
            currentJoinStat = resultStat; currentStat = rows.result;
        }

        MAddExclMap<WhereJoin, Where> reducedUpWheres = MapFact.mAddExclMap(upWheres);
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
                    reducedUpWheres.exclAdd(joinFollow, reduceFollowUpWheres.result.get(joinFollow));
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
        
        if(!currentJoinStat.less(startJoinStat))
            return null;
        
        if(Stat.ALOT.lessEquals(currentStat))
            return null;

        Where result = Where.TRUE;
        for (WhereJoin where : current)
            result = result.and(reducedUpWheres.get(where)).and(BaseExpr.getOrWhere(where)); // чтобы не потерять or, конкретного единично кейса нет, но с логической точки зрения
        return result;
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
            if (!remove && whereJoin instanceof ExprOrderTopJoin && ((ExprOrderTopJoin)whereJoin).givesNoKeys()) // даст висячий ключ при проталкивании, вообще рекурсивно пойти не может, но смысла нет разбирать
                remove = true;
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

    public <K extends Expr> Where getGroupPushWhere(ImMap<K, BaseExpr> joinMap, ImMap<WhereJoin, Where> upWheres, QueryJoin<K, ?, ?, ?> skipJoin, KeyStat keyStat, Stat currentStat, Stat currentJoinStat) {
        Where pushWhere = getPushWhere(joinMap, upWheres, skipJoin, keyStat, currentStat, currentJoinStat);
        if(pushWhere!=null) {
            return GroupExpr.create(joinMap, pushWhere, joinMap.keys().toMap()).getWhere();
        } else
            return null;
    }


    public Where getPartitionPushWhere(ImMap<KeyExpr, BaseExpr> joinMap, ImSet<Expr> partitions, ImMap<WhereJoin, Where> upWheres, QueryJoin<KeyExpr, ?, ?, ?> skipJoin, KeyStat keyStat, Stat currentStat, Stat currentJoinStat) {
        joinMap = joinMap.filterIncl(BaseUtils.<ImSet<KeyExpr>>immutableCast(AbstractOuterContext.getOuterSetKeys(partitions))); // так как в partitions могут быть не все ключи, то в явную добавим условия на не null для таких ключей
        Where pushWhere = getPushWhere(joinMap, upWheres, skipJoin, keyStat, currentStat, currentJoinStat);
        if(pushWhere!=null) {
            ImMap<Expr, Expr> partMap = partitions.toMap();
            return GroupExpr.create(new QueryTranslator(joinMap).translate(partMap), pushWhere, partMap).getWhere();
        } else
            return null;
    }
    
    // получает подможнство join'ов которое дает joinKeys, пропуская skipJoin. тут же алгоритм по определению достаточных ключей
    // !!! ТЕОРЕТИЧЕСКИ НЕСМОТРЯ НА REMOVE из-за паковки может проталкивать бесконечно (впоследствии нужен будет GUARD), например X = L(G1 + G2) AND (G1 OR G2) спакуется в X = L(G1 + G2) AND (G1' OR G2) , (а не L(G1' + G2), и будет G1 проталкивать бесконечно) 
    //  но это очень редкая ситуация и важно проследить за ее природой, так как возможно есть аналогичные assertion'ы  
    public <K extends Expr> Where getPushWhere(ImMap<K, BaseExpr> joinKeys, ImMap<WhereJoin, Where> upWheres, QueryJoin<K, ?, ?, ?> skipJoin, KeyStat keyStat, Stat currentStat, Stat currentJoinStat) {
        // joinKeys из skipJoin.getJoins()

        assert joinKeys.equals(skipJoin.getJoins().filterIncl(joinKeys.keys()));
        Result<ImMap<WhereJoin, Where>> upFitWheres = new Result<ImMap<WhereJoin, Where>>();
        WhereJoins removedJoins = removeJoin(skipJoin, upWheres, upFitWheres);
        if(removedJoins==null) {
            removedJoins = this;
            upFitWheres.set(upWheres);
        }
        return removedJoins.getPushWhere(joinKeys.values().toSet(), upFitWheres.result, keyStat, currentStat, currentJoinStat);
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
    public Where fillInnerJoins(ImMap<WhereJoin, Where> upWheres, MList<String> whereSelect, CompileSource source) {
        Where innerWhere = Where.TRUE;
        for (WhereJoin where : wheres)
            if(!(where instanceof ExprOrderTopJoin && ((ExprOrderTopJoin)where).givesNoKeys())) {
                Where upWhere = upWheres.get(where);
                String upSource = upWhere.getSource(source);
                if(where instanceof ExprJoin && ((ExprJoin)where).isClassJoin()) {
                    whereSelect.add(upSource);
                    innerWhere = innerWhere.and(upWhere);
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
