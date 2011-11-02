package platform.server.data.query.stat;

import platform.base.AddSet;
import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.Result;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.*;
import platform.server.data.expr.query.*;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.SourceJoin;
import platform.server.data.query.innerjoins.KeyEqual;
import platform.server.data.translator.HashLazy;
import platform.server.data.translator.HashOuterLazy;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;

import java.util.*;

public class WhereJoins extends AddSet<WhereJoin, WhereJoins> implements DNFWheres.Interface<WhereJoins>, OuterContext<WhereJoins> {

    public WhereJoins() {
    }

    public WhereJoins(WhereJoin[] wheres) {
        super(wheres);
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

    @HashOuterLazy
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

    public SourceJoin[] getEnum() {
        SourceJoin[][] enums = new SourceJoin[wheres.length][]; int tot = 0;
        for(int i=0;i<wheres.length;i++) {
            enums[i] = wheres[i].getEnum();
            tot += enums[i].length;
        }
        SourceJoin[] result = new SourceJoin[tot]; int wr = 0;
        for(int i=0;i<wheres.length;i++) {
            System.arraycopy(enums[i], 0, result, wr, enums[i].length);
            wr += enums[i].length;
        }
        return result;
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

    public <K extends BaseExpr> StatKeys<K> getStatKeys(Set<K> groups, KeyStat keyStat) {
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
        return new StatKeys<K>(distinct.getMax().min(rowStat), distinct); // возвращаем min(суммы groups, расчитанного результата)
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(Set<K> groups, Map<WhereJoin, Where> upWheres, WhereBuilder result, KeyStat stat) {
        // нужно попытаться опускаться ниже, устраняя "избыточные" WhereJoin'ы или InnerJoin'ы

        StatKeys<K> resultStat = getStatKeys(groups, stat);
        List<WhereJoin> current = BaseUtils.toList(wheres);

        Map<WhereJoin, Where> reducedUpWheres = new HashMap<WhereJoin, Where>(upWheres);
        int it = 0;
        while(it < current.size()) {
            WhereJoin<?, ?> reduceJoin = current.get(it);

            List<WhereJoin> reduced = new ArrayList<WhereJoin>(current);
            reduced.remove(it);

            Result<Map<InnerJoin, Where>> reduceFollowUpWheres = new Result<Map<InnerJoin, Where>>();
            for(InnerJoin joinFollow : reduceJoin.getJoinFollows(reduceFollowUpWheres)) { // пытаемся заменить reduceJoin, на его joinFollows
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

            StatKeys<K> reducedStat = new WhereJoins(reduced.toArray(new WhereJoin[reduced.size()])).getStatKeys(groups, stat);
            assert !reducedStat.rows.less(resultStat.rows); // вообще это не правильный assertion, потому как если уходит ключ статистика может уменьшиться
            if(reducedStat.rows.equals(resultStat.rows))
                current = reduced;
            else
                it++;
        }
        result.add(new WhereJoins(current.toArray(new WhereJoin[current.size()])).getWhere(reducedUpWheres));

        return resultStat;
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(Set<K> groups, final KeyStat keyStat, final KeyEqual keyEqual) {
        if(!keyEqual.isEmpty()) { // для оптимизации
            return and(keyEqual.getWhereJoins()).getStatKeys(groups, new KeyStat() {
                public Stat getKeyStat(KeyExpr key) {
                    BaseExpr keyExpr = keyEqual.keyExprs.get(key);
                    if(keyExpr!=null)
                        return keyExpr.getTypeStat(keyStat);
                    else
                        return keyStat.getKeyStat(key);
                }
            });
        } else
            return getStatKeys(groups, keyStat);
    }

    public WhereJoins removeJoin(WhereJoin join, Map<WhereJoin, Where> upWheres, Map<WhereJoin, Where> upFitWheres) {
        List<WhereJoin> fitWheres = new ArrayList<WhereJoin>();
        for(WhereJoin where : wheres) {
            if(!containsAll(where, join)) { // не интересуют те из которых следует этот join (потому как рекурсия будет)
                fitWheres.add(where);
                upFitWheres.put(where, upWheres.get(where));
            }
        }
        return new WhereJoins(fitWheres.toArray(new WhereJoin[fitWheres.size()]));
    }

    // получает подможнство join'ов которое дает joinKeys, пропуская skipJoin. тут же алгоритм по определению достаточных ключей
    public <K extends Expr> StatKeys<K> getStatKeys(Set<K> joinKeys, Map<WhereJoin, Where> upWheres, QueryJoin<K, ?> skipJoin, WhereBuilder result, KeyStat keyStat) {
        // joinKeys из skipJoin.getJoins()

        Map<BaseExpr, K> groupKeys = BaseUtils.reverse(BaseUtils.filterKeys(skipJoin.getJoins(), joinKeys));
        Map<WhereJoin, Where> upFitWheres = new HashMap<WhereJoin, Where>();
        return removeJoin(skipJoin, upWheres, upFitWheres).getStatKeys(groupKeys.keySet(), upFitWheres, result, keyStat).map(groupKeys);
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

    public Where getWhere(Map<WhereJoin, Where> upWheres) {
        Where result = Where.TRUE;
        for (WhereJoin where : wheres)
            result = result.and(upWheres.get(where));
        return result;
    }

    @IdentityLazy
    public Set<Value> getOuterValues() {
        return AbstractSourceJoin.getOuterValues(this);
    }
}
