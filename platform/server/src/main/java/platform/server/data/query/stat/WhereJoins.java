package platform.server.data.query.stat;

import platform.base.*;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.ManualLazy;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.*;
import platform.server.data.expr.query.*;
import platform.server.data.query.*;
import platform.server.data.query.innerjoins.KeyEqual;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.Where;

import java.util.*;

import static platform.base.BaseUtils.filterKeys;
import static platform.base.BaseUtils.immutableCast;
import static platform.base.BaseUtils.toMap;

public class WhereJoins extends AddSet<WhereJoin, WhereJoins> implements DNFWheres.Interface<WhereJoins>, OuterContext<WhereJoins> {

    public WhereJoins() {
    }

    public WhereJoins(WhereJoin[] wheres) {
        super(wheres);
    }

    public WhereJoins(Collection<WhereJoin> wheres) {
        super(wheres.toArray(new WhereJoin[wheres.size()]));
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
            innerJoins = new InnerJoins();
            for(WhereJoin where : wheres)
                innerJoins = innerJoins.and(where.getInnerJoins());
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

    public QuickSet<OuterContext> getOuterDepends() {
        return new QuickSet<OuterContext>(wheres);
    }

    private static class Edge<K> {
        public BaseJoin<K> join;
        public K key;

        public Stat getKeyStat(Map<BaseJoin, Stat> statJoins, KeyStat keyStat) {
            return join.getStatKeys(keyStat).distinct.get(key).min(statJoins.get(join));
        }
        public Stat getPropStat(Map<BaseJoin, Stat> joinStats, KeyStat keyStat, Map<BaseExpr, Stat> propStats) {
            return WhereJoins.getPropStat(getPropExpr(), joinStats, keyStat, propStats);
        }
        public BaseExpr getPropExpr() {
            return join.getJoins().get(key);
        }

        private Edge(BaseJoin<K> join, K key) {
            this.join = join;
            this.key = key;
        }
    }

    private static Stat getPropStat(BaseExpr valueExpr, KeyStat keyStat, Map<BaseExpr, Stat> propStats) {
        Stat result = propStats.get(valueExpr);
        if(result==null)
            result = valueExpr.getStatValue(keyStat);
        return result;
    }

    private static Stat getPropStat(BaseExpr valueExpr, Map<BaseJoin, Stat> joinStats, KeyStat keyStat, Map<BaseExpr, Stat> propStats) {
        return getPropStat(valueExpr, keyStat, propStats).min(joinStats.get(valueExpr.getBaseJoin()));
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(QuickSet<K> groups, KeyStat keyStat) {
        return getStatKeys(groups, null, keyStat);        
    }

    // assert что rows >= result
    public <K extends BaseExpr> StatKeys<K> getStatKeys(QuickSet<K> groups, Result<Stat> rows, KeyStat keyStat) {
        Map<BaseJoin, Stat> statJoins = new HashMap<BaseJoin, Stat>();
        Map<BaseExpr, Stat> propStats = new HashMap<BaseExpr, Stat>();

        Set<Edge> edges = new HashSet<Edge>();

        // собираем все ребра и вершины
        Set<BaseJoin> joins = new HashSet<BaseJoin>();
        Queue<BaseJoin> queue = new LinkedList<BaseJoin>();
        for(WhereJoin valueJoin : wheres) {
            queue.add(valueJoin);
            joins.add(valueJoin);
        }
        for(BaseExpr group : groups) {
            InnerBaseJoin<?> valueJoin = group.getBaseJoin();
            if(!joins.contains(valueJoin)) {
                queue.add(valueJoin);
                joins.add(valueJoin);
            }
        }
        while(!queue.isEmpty()) {
            BaseJoin<?> join = queue.poll();
            for(Map.Entry<?, BaseExpr> joinExpr : join.getJoins().entrySet()) {
                edges.add(new Edge(join, joinExpr.getKey()));
                InnerBaseJoin<?> valueJoin = joinExpr.getValue().getBaseJoin();
                if(!joins.contains(valueJoin)) {
                    queue.add(valueJoin);
                    joins.add(valueJoin);
                }
            }
        }

        for(BaseJoin join : joins)
            statJoins.put(join, join.getStatKeys(keyStat).rows);

        // ищем несбалансированное ребро с минимальной статистикой
        Stat balanced = Stat.ONE;
        while(edges.size() > 0) {
            Map<Edge, Pair<Stat, Stat>> minEdges = new HashMap<Edge, Pair<Stat, Stat>>();
            Stat stat = Stat.MAX;
            for(Edge edge : edges) {
                Stat keys = edge.getKeyStat(statJoins, keyStat);
                Stat values = edge.getPropStat(statJoins, keyStat, propStats);
                Stat min = keys.min(values);
                if(min.less(stat)) { // если нашли новый минимум про старый забываем
                    minEdges = new HashMap<Edge, Pair<Stat, Stat>>();
                    stat = min;
                }
                if(min.equals(stat))
                    minEdges.put(edge, new Pair<Stat, Stat>(keys, values));
            }
            Edge<?> unbalancedEdge = null; Pair<Stat, Stat> unbalancedStat = null;
            for(Map.Entry<Edge, Pair<Stat, Stat>> edge : minEdges.entrySet()) {  // выкидываем все сбалансированные с такой статистикой
                if(edge.getValue().first.equals(edge.getValue().second)) {
                    balanced = balanced.mult(edge.getValue().first);
                    edges.remove(edge.getKey());
                } else {
                    unbalancedEdge = edge.getKey();
                    unbalancedStat = edge.getValue();
                }
            }
            if(unbalancedEdge!=null) {
                Stat balancedStat;
                if(unbalancedStat.first.less(unbalancedStat.second)) { // балансируем значение
                    Stat decrease = unbalancedStat.second.div(unbalancedStat.first);
                    BaseExpr baseExpr = unbalancedEdge.getPropExpr();
                    propStats.put(baseExpr, unbalancedStat.first); // это и есть разница
                    BaseJoin valueJoin = baseExpr.getBaseJoin();
                    statJoins.put(valueJoin, statJoins.get(valueJoin).div(decrease));
                    balancedStat = unbalancedStat.first;
                } else { // балансируем ключ, больше он использовать
                    Stat decrease = unbalancedStat.first.div(unbalancedStat.second);
                    statJoins.put(unbalancedEdge.join, statJoins.get(unbalancedEdge.join).div(decrease));
                    balancedStat = unbalancedStat.second;
                }
                balanced = balanced.mult(balancedStat);
                edges.remove(unbalancedEdge);
            }
        }

        // бежим по всем сбалансированным ребрам суммируем, бежим по всем нодам суммируем, возвращаем разность
        Stat rowStat = Stat.ONE;
        for(Stat joinStat : statJoins.values())
            rowStat = rowStat.mult(joinStat);
        rowStat = rowStat.div(balanced);

        DistinctKeys<K> distinct = new DistinctKeys<K>();
        for(K group : groups) // для groups, берем min(из статистики значения, статистики его join'а)
            distinct.add(group, getPropStat(group, statJoins, keyStat, propStats).min(rowStat));
        
        if(rows!=null)
            rows.set(rowStat);
        return new StatKeys<K>(distinct.getMax().min(rowStat), distinct); // возвращаем min(суммы groups, расчитанного результата)
    }

    public <K extends BaseExpr> Where getPushWhere(QuickSet<K> groups, Map<WhereJoin, Where> upWheres, KeyStat stat, Stat currentStat, Stat currentJoinStat) {
        // нужно попытаться опускаться ниже, устраняя "избыточные" WhereJoin'ы или InnerJoin'ы

        List<WhereJoin> current;
        Result<Stat> rows = new Result<Stat>();
        Stat resultStat = getStatKeys(groups, rows, stat).rows;
        if(resultStat.less(currentJoinStat) && rows.result.lessEquals(currentStat)) {
            currentJoinStat = resultStat; currentStat = rows.result; current = BaseUtils.toList(wheres);
        } else // если ключей больше чем в исходном или статистика увеличилась
            return null;

        Map<WhereJoin, Where> reducedUpWheres = new HashMap<WhereJoin, Where>(upWheres);
        int it = 0;
        while(it < current.size()) {
            WhereJoin<?, ?> reduceJoin = current.get(it);

            List<WhereJoin> reduced = new ArrayList<WhereJoin>(current);
            reduced.remove(it);

            Result<Map<InnerJoin, Where>> reduceFollowUpWheres = new Result<Map<InnerJoin, Where>>();
            for(InnerJoin joinFollow : reduceJoin.getJoinFollows(reduceFollowUpWheres, null)) { // пытаемся заменить reduceJoin, на его joinFollows
                boolean found = false;
                for(WhereJoin andJoin : reduced)
                    if(containsAll(andJoin, joinFollow)) {
                        found = true;
                        break;
                    }
                if(!found) {
                    reduced.add(joinFollow);
                    reducedUpWheres.put(joinFollow, reduceFollowUpWheres.result.get(joinFollow));
                }
            }

            WhereJoins reducedJoins = new WhereJoins(reduced);
            rows = new Result<Stat>();
            Stat reducedStat = reducedJoins.getStatKeys(groups, rows, stat).rows;
//            assert !reducedJoins.getStatKeys(groups, stat).rows.less(resultStat.rows); // вообще это не правильный assertion, потому как если уходит ключ статистика может уменьшиться
            if(reducedStat.lessEquals(currentJoinStat) && rows.result.lessEquals(currentStat)) { // сколько сгруппировать надо
                currentJoinStat = reducedStat; currentStat = rows.result; current = reduced;
            } else
                it++;
        }
        
        if(Stat.ALOT.lessEquals(currentStat))
            return null;

        Where result = Where.TRUE;
        for (WhereJoin where : current)
            result = result.and(reducedUpWheres.get(where));
        return result;
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(QuickSet<K> groups, final KeyStat keyStat, final KeyEqual keyEqual) {
        if(!keyEqual.isEmpty()) { // для оптимизации
            return and(keyEqual.getWhereJoins()).getStatKeys(groups, keyEqual.getKeyStat(keyStat));
        } else
            return getStatKeys(groups, keyStat);
    }

    public static <T extends WhereJoin> WhereJoins removeJoin(QueryJoin removeJoin, WhereJoin[] wheres, Map<WhereJoin, Where> upWheres, Result<Map<WhereJoin, Where>> resultWheres) {
        WhereJoins result = null;
        Map<WhereJoin, Where> resultUpWheres = null;
        Collection<WhereJoin> keepWheres = new ArrayList<WhereJoin>();
        for(WhereJoin whereJoin : wheres) {
            WhereJoins removeJoins;
            Result<Map<WhereJoin, Where>> removeUpWheres = new Result<Map<WhereJoin, Where>>();

            boolean remove = BaseUtils.hashEquals(removeJoin, whereJoin);
            InnerJoins joinFollows = null; Result<Map<InnerJoin, Where>> joinUpWheres = null;
            if(!remove) {
                Set<UnionJoin> unionJoins = new HashSet<UnionJoin>();
                joinUpWheres = new Result<Map<InnerJoin, Where>>();
                joinFollows = whereJoin.getJoinFollows(joinUpWheres, unionJoins);
                for(UnionJoin unionJoin : unionJoins) // без этой проверку может бесконечно проталкивать
                    if(unionJoin.depends(removeJoin)) {
                        remove = true;
                        break;
                    }
            }

            if(remove) {
                removeJoins = new WhereJoins();
                removeUpWheres.set(new HashMap<WhereJoin, Where>());
            } else
                removeJoins = joinFollows.removeJoin(removeJoin,
                        BaseUtils.<Map<WhereJoin,Where>>immutableCast(joinUpWheres.result), removeUpWheres);

            if(removeJoins!=null) { // вырезали, придется выкидывать целиком join, оставлять sibling'ом
                if(result==null) {
                    result = removeJoins;
                    resultUpWheres = removeUpWheres.result;
                } else {
                    result = result.and(removeJoins);
                    resultUpWheres = result.andUpWheres(resultUpWheres, removeUpWheres.result);
                }
            } else
                keepWheres.add(whereJoin);
        }

        if(result!=null) {
            result = result.and(new WhereJoins(keepWheres));
            resultWheres.set(result.andUpWheres(resultUpWheres, filterKeys(upWheres, keepWheres)));
            return result;
        }
        return null;
    }

    // устраняет сам join чтобы при проталкивании не было рекурсии
    public WhereJoins removeJoin(QueryJoin join, Map<WhereJoin, Where> upWheres, Result<Map<WhereJoin, Where>> resultWheres) {
        return removeJoin(join, wheres, upWheres, resultWheres);
    }

    public <K extends Expr> Where getGroupPushWhere(Map<K, BaseExpr> joinMap, Map<WhereJoin, Where> upWheres, QueryJoin<K, ?, ?, ?> skipJoin, KeyStat keyStat, Stat currentStat, Stat currentJoinStat) {
        Where pushWhere = getPushWhere(joinMap, upWheres, skipJoin, keyStat, currentStat, currentJoinStat);
        if(pushWhere!=null) {
            return GroupExpr.create(joinMap, pushWhere, BaseUtils.toMap(joinMap.keySet())).getWhere();
        } else
            return null;
    }


    public Where getPartitionPushWhere(Map<KeyExpr, BaseExpr> joinMap, Set<Expr> partitions, Map<WhereJoin, Where> upWheres, QueryJoin<KeyExpr, ?, ?, ?> skipJoin, KeyStat keyStat, Stat currentStat, Stat currentJoinStat) {
        joinMap = BaseUtils.filterKeys(joinMap, AbstractOuterContext.getOuterKeys(partitions)); // так как в partitions могут быть не все ключи, то в явную добавим условия на не null для таких ключей
        Where pushWhere = getPushWhere(joinMap, upWheres, skipJoin, keyStat, currentStat, currentJoinStat);
        if(pushWhere!=null) {
            return GroupExpr.create(new QueryTranslator(joinMap).translate(toMap(partitions)), pushWhere, BaseUtils.toMap(partitions)).getWhere();
        } else
            return null;
    }
    
    // получает подможнство join'ов которое дает joinKeys, пропуская skipJoin. тут же алгоритм по определению достаточных ключей
    public <K extends Expr> Where getPushWhere(Map<K, BaseExpr> joinKeys, Map<WhereJoin, Where> upWheres, QueryJoin<K, ?, ?, ?> skipJoin, KeyStat keyStat, Stat currentStat, Stat currentJoinStat) {
        // joinKeys из skipJoin.getJoins()

        assert joinKeys.equals(BaseUtils.filterKeys(skipJoin.getJoins(), joinKeys.keySet()));
        Result<Map<WhereJoin, Where>> upFitWheres = new Result<Map<WhereJoin, Where>>();
        WhereJoins removedJoins = removeJoin(skipJoin, upWheres, upFitWheres);
        if(removedJoins==null) {
            removedJoins = this;
            upFitWheres.set(upWheres);
        }
        return removedJoins.getPushWhere(new QuickSet<BaseExpr>(joinKeys.values()), upFitWheres.result, keyStat, currentStat, currentJoinStat);
    }

    // может как MeanUpWheres сделать
    public static <J extends WhereJoin> Map<J, Where> andUpWheres(J[] wheres, Map<J, Where> up1, Map<J, Where> up2) {
        Map<J, Where> result = new HashMap<J, Where>();
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
            result.put(where, andWhere);
        }
        return result;
    }

    public Map<WhereJoin, Where> andUpWheres(Map<WhereJoin, Where> up1, Map<WhereJoin, Where> up2) {
        return andUpWheres(wheres, up1, up2);
    }

    public Map<WhereJoin, Where> orUpWheres(Map<WhereJoin, Where> up1, Map<WhereJoin, Where> up2) {
        Map<WhereJoin, Where> result = new HashMap<WhereJoin, Where>();
        for(WhereJoin where : wheres)
            result.put(where, up1.get(where).or(up2.get(where)));
        return result;
    }

    // из upMeans следует
    public Map<WhereJoin, Where> orMeanUpWheres(Map<WhereJoin, Where> up, WhereJoins meanWheres, Map<WhereJoin, Where> upMeans) {
        Map<WhereJoin, Where> result = new HashMap<WhereJoin, Where>();
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
            result.put(where, up.get(where).or(up2Where));
        }
        return result;
    }

    public Where fillInnerJoins(Map<WhereJoin, Where> upWheres, Collection<String> whereSelect, CompileSource source) {
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

    public QuickSet<KeyExpr> getOuterKeys() {
        return AbstractOuterContext.getOuterKeys(this);
    }

    public QuickSet<Value> getOuterValues() {
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
}
