package platform.server.data.query.stat;

import platform.base.AddSet;
import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.Result;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MCol;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.MExclSet;
import platform.base.col.interfaces.mutable.add.MAddExclMap;
import platform.base.col.interfaces.mutable.add.MAddMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.ManualLazy;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.InnerExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.DistinctKeys;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.QueryJoin;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.*;
import platform.server.data.query.innerjoins.KeyEqual;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.Where;

import java.util.*;

public class WhereJoins extends AddSet<WhereJoin, WhereJoins> implements DNFWheres.Interface<WhereJoins>, OuterContext<WhereJoins> {

    public WhereJoins() {
    }

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

    public ImSet<OuterContext> getOuterDepends() {
        return SetFact.<OuterContext>toExclSet(wheres);
    }

    private static class Edge<K> {
        public BaseJoin<K> join;
        public K key;

        public Stat getKeyStat(MAddMap<BaseJoin, Stat> statJoins, KeyStat keyStat) {
            return join.getStatKeys(keyStat).distinct.get(key).min(statJoins.get(join));
        }
        public Stat getPropStat(MAddMap<BaseJoin, Stat> joinStats, KeyStat keyStat, MAddMap<BaseExpr, Stat> propStats) {
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

    private static Stat getPropStat(BaseExpr valueExpr, KeyStat keyStat, MAddMap<BaseExpr, Stat> propStats) {
        Stat result = propStats.get(valueExpr);
        if(result==null)
            result = valueExpr.getStatValue(keyStat);
        return result;
    }

    private static Stat getPropStat(BaseExpr valueExpr, MAddMap<BaseJoin, Stat> joinStats, KeyStat keyStat, MAddMap<BaseExpr, Stat> propStats) {
        return getPropStat(valueExpr, keyStat, propStats).min(joinStats.get(valueExpr.getBaseJoin()));
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, KeyStat keyStat) {
        return getStatKeys(groups, null, keyStat);        
    }

    // assert что rows >= result
    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups, Result<Stat> rows, final KeyStat keyStat) {
        final MAddMap<BaseJoin, Stat> statJoins = MapFact.mAddOverrideMap();
        final MAddMap<BaseExpr, Stat> propStats = MapFact.mAddOverrideMap();

        Set<Edge> edges = SetFact.mAddRemoveSet();

        // собираем все ребра и вершины
        Set<BaseJoin> joins = SetFact.mAddRemoveSet();
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
            ImMap<?, BaseExpr> joinExprs = join.getJoins();
            for(int i=0,size=joinExprs.size();i<size;i++) {
                edges.add(new Edge(join, joinExprs.getKey(i)));
                InnerBaseJoin<?> valueJoin = joinExprs.getValue(i).getBaseJoin();
                if(!joins.contains(valueJoin)) {
                    queue.add(valueJoin);
                    joins.add(valueJoin);
                }
            }
        }

        for(BaseJoin join : joins)
            statJoins.add(join, join.getStatKeys(keyStat).rows);

        // ищем несбалансированное ребро с минимальной статистикой
        Stat balanced = Stat.ONE;
        while(edges.size() > 0) {
            MAddExclMap<Edge, Pair<Stat, Stat>> minEdges = MapFact.mAddExclMap();
            Stat stat = Stat.MAX;
            for(Edge edge : edges) {
                Stat keys = edge.getKeyStat(statJoins, keyStat);
                Stat values = edge.getPropStat(statJoins, keyStat, propStats);
                Stat min = keys.min(values);
                if(min.less(stat)) { // если нашли новый минимум про старый забываем
                    minEdges = MapFact.mAddExclMap();
                    stat = min;
                }
                if(min.equals(stat))
                    minEdges.exclAdd(edge, new Pair<Stat, Stat>(keys, values));
            }
            Edge<?> unbalancedEdge = null; Pair<Stat, Stat> unbalancedStat = null;
            for(int i=0,size=minEdges.size();i<size;i++) {  // выкидываем все сбалансированные с такой статистикой
                Pair<Stat, Stat> minStat = minEdges.getValue(i);
                if(minStat.first.equals(minStat.second)) {
                    balanced = balanced.mult(minStat.first);
                    edges.remove(minEdges.getKey(i));
                } else {
                    unbalancedEdge = minEdges.getKey(i);
                    unbalancedStat = minStat;
                }
            }
            if(unbalancedEdge!=null) {
                Stat balancedStat;
                if(unbalancedStat.first.less(unbalancedStat.second)) { // балансируем значение
                    Stat decrease = unbalancedStat.second.div(unbalancedStat.first);
                    BaseExpr baseExpr = unbalancedEdge.getPropExpr();
                    propStats.add(baseExpr, unbalancedStat.first); // это и есть разница
                    BaseJoin valueJoin = baseExpr.getBaseJoin();
                    statJoins.add(valueJoin, statJoins.get(valueJoin).div(decrease));
                    balancedStat = unbalancedStat.first;
                } else { // балансируем ключ, больше он использовать
                    Stat decrease = unbalancedStat.first.div(unbalancedStat.second);
                    statJoins.add(unbalancedEdge.join, statJoins.get(unbalancedEdge.join).div(decrease));
                    balancedStat = unbalancedStat.second;
                }
                balanced = balanced.mult(balancedStat);
                edges.remove(unbalancedEdge);
            }
        }

        // бежим по всем сбалансированным ребрам суммируем, бежим по всем нодам суммируем, возвращаем разность
        Stat rowStat = Stat.ONE;
        for(int i=0,size=statJoins.size();i<size;i++)
            rowStat = rowStat.mult(statJoins.getValue(i));
        final Stat finalStat = rowStat.div(balanced);
        if(rows!=null)
            rows.set(finalStat);

        DistinctKeys<K> distinct = new DistinctKeys<K>(groups.mapValues(new GetValue<Stat, K>() {
            public Stat getMapValue(K value) { // для groups, берем min(из статистики значения, статистики его join'а)
                return getPropStat(value, statJoins, keyStat, propStats).min(finalStat);
            }}));
        return new StatKeys<K>(distinct.getMax().min(finalStat), distinct); // возвращаем min(суммы groups, расчитанного результата)
    }

    public <K extends BaseExpr> Where getPushWhere(ImSet<K> groups, ImMap<WhereJoin, Where> upWheres, KeyStat stat, Stat currentStat, Stat currentJoinStat) {
        // нужно попытаться опускаться ниже, устраняя "избыточные" WhereJoin'ы или InnerJoin'ы

        List<WhereJoin> current;
        Result<Stat> rows = new Result<Stat>();
        Stat resultStat = getStatKeys(groups, rows, stat).rows;
        if(resultStat.less(currentJoinStat) && rows.result.lessEquals(currentStat)) {
            currentJoinStat = resultStat; currentStat = rows.result; current = BaseUtils.toList(wheres);
        } else // если ключей больше чем в исходном или статистика увеличилась
            return null;

        MAddExclMap<WhereJoin, Where> reducedUpWheres = MapFact.mAddExclMap(upWheres);
        int it = 0;
        while(it < current.size()) {
            WhereJoin<?, ?> reduceJoin = current.get(it);

            List<WhereJoin> reduced = new ArrayList<WhereJoin>(current);
            reduced.remove(it);

            Result<ImMap<InnerJoin, Where>> reduceFollowUpWheres = new Result<ImMap<InnerJoin, Where>>();
            for(InnerJoin joinFollow : reduceJoin.getJoinFollows(reduceFollowUpWheres, null)) { // пытаемся заменить reduceJoin, на его joinFollows
                boolean found = false;
                for(WhereJoin andJoin : reduced)
                    if(containsAll(andJoin, joinFollow)) {
                        found = true;
                        break;
                    }
                if(!found) {
                    reduced.add(joinFollow);
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
                removeJoins = new WhereJoins();
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
        joinMap = joinMap.filterIncl(AbstractOuterContext.getOuterKeys(partitions)); // так как в partitions могут быть не все ключи, то в явную добавим условия на не null для таких ключей
        Where pushWhere = getPushWhere(joinMap, upWheres, skipJoin, keyStat, currentStat, currentJoinStat);
        if(pushWhere!=null) {
            ImMap<Expr, Expr> partMap = partitions.toMap();
            return GroupExpr.create(new QueryTranslator(joinMap).translate(partMap), pushWhere, partMap).getWhere();
        } else
            return null;
    }
    
    // получает подможнство join'ов которое дает joinKeys, пропуская skipJoin. тут же алгоритм по определению достаточных ключей
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

    public Where fillInnerJoins(ImMap<WhereJoin, Where> upWheres, MCol<String> whereSelect, CompileSource source) {
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

    public ImSet<KeyExpr> getOuterKeys() {
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
}
