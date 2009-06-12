package platform.server.data.query;

import platform.base.BaseUtils;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.query.exprs.cases.MapCase;
import platform.server.data.query.translators.MergeTranslator;
import platform.server.data.query.translators.PackTranslator;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.sql.SQLSyntax;
import platform.server.where.Where;
import platform.server.caches.Lazy;
import platform.server.caches.SynchronizedLazy;

import java.util.*;

import net.jcip.annotations.Immutable;

@Immutable
class ParsedJoinQuery<K,V> implements ParsedQuery<K,V>,MapKeysInterface<K> {

    public final Map<K,KeyExpr> mapKeys;
    public final Map<V, SourceExpr> properties;
    protected final Where where;

    public Map<K, KeyExpr> getMapKeys() {
        return mapKeys;
    }

    protected final Context context;

    ParsedJoinQuery(JoinQuery<K,V> query) {
        mapKeys = query.mapKeys;

        // сливаем joins
        MergeTranslator merge = new MergeTranslator(query.getContext());
        Where<?> mergedWhere = query.where.translate(merge);

        // упаковка общим where, для packed транслятора сначала делаем чтобы не нарушить assertion с JoinExpr
        Map<V,SourceExpr> packedProps = mergedWhere.followTrue(merge.translate(query.properties));

        // узнаем все join'ы
        Context packedContext = JoinQuery.getContext(mapKeys,packedProps,mergedWhere);

        PackTranslator pack = new PackTranslator(packedContext,mergedWhere);
        context = pack.context;
        where = mergedWhere.translate(pack);
        properties = pack.translate(packedProps);
    }

/*    CompiledQuery<K,V> compile = null;
    LinkedHashMap<V,Boolean> compileOrders;
    int compileTop;*/

    public CompiledQuery<K,V> compileSelect(SQLSyntax syntax) {
        return compileSelect(syntax,new LinkedHashMap<V, Boolean>(),0);
    }
    @SynchronizedLazy
    public CompiledQuery<K,V> compileSelect(SQLSyntax syntax,LinkedHashMap<V,Boolean> orders,int top) {
/*        synchronized(this) { // тут он уже в кэше может быть
            if(compile==null || !(compileOrders.equals(orders) && compileTop==top)) {
                compile = new CompiledQuery<K,V>(this, syntax, orders, top);
                compileOrders = orders;
                compileTop = top;
            }
            return compile;
        }*/
        return new CompiledQuery<K,V>(this, syntax, orders, top);
    }

    public Join<V> join(Map<K, ? extends SourceExpr> joinImplement) {
        assert joinImplement.size()==mapKeys.size();
        return join(joinImplement,BaseUtils.toMap(context.values));
    }

    public Join<V> join(Map<K, ? extends SourceExpr> joinImplement,Map<ValueExpr,ValueExpr> mapValues) { // последний параметр = какой есть\какой нужно
        assert joinImplement.size()==mapKeys.size();

        Map<K,KeyExpr> joinKeys = new HashMap<K, KeyExpr>();
        for(Map.Entry<K,? extends SourceExpr> joinExpr : joinImplement.entrySet()) {
            if(!(joinExpr.getValue() instanceof KeyExpr) || joinKeys.values().contains((KeyExpr)joinExpr.getValue()))
                return joinExprs(joinImplement, mapValues);
           joinKeys.put(joinExpr.getKey(), (KeyExpr) joinExpr.getValue());
        }
        return new TranslateJoin<V>(new KeyTranslator(context, BaseUtils.crossJoin(mapKeys, joinKeys), mapValues), this);
    }

    public Join<V> joinExprs(Map<K, ? extends SourceExpr> joinImplement,Map<ValueExpr,ValueExpr> mapValues) { // последний параметр = какой есть\какой нужно
        assert joinImplement.size()==mapKeys.size();

        Where joinWhere = Where.TRUE; // надо еще where join'ов закинуть
        for(SourceExpr joinExpr : joinImplement.values())
            joinWhere = joinWhere.and(joinExpr.getWhere());
        return new CaseJoin<V>(joinWhere, new TranslateJoin<V>(new QueryTranslator(context, BaseUtils.crossJoin(mapKeys, joinImplement), mapValues), this));
    }

    public ParsedJoinQuery(Context iContext,Map<K,KeyExpr> iMapKeys,Map<V,SourceExpr> iProperties, Where iWhere) { // для groupQuery full join'ов
        mapKeys = iMapKeys;

        properties = iProperties;
        where = iWhere;

        context = iContext;
    }

    // для заданных свойств вытягивает условия на классы для них
    public <B> ClassWhere<B> getClassWhere(Collection<? extends V> classProps) {
        Where upWhere = where.not();

        ClassWhere<B> result = new ClassWhere<B>();
        for(MapCase<? extends V> mapCase : CaseExpr.pullCases(BaseUtils.filterKeys(properties,classProps))) {
            Where caseWhere = mapCase.where.and(upWhere.not());
            for(AndExpr expr : mapCase.data.values()) // все следствия за and'им
                caseWhere = caseWhere.and(expr.getWhere());

            result = result.or(ClassWhere.get(BaseUtils.<B,AndExpr>forceMerge(mapCase.data,mapKeys),caseWhere));

            upWhere = upWhere.or(mapCase.where);
        }
        return result;
    }

    @Lazy
    public SourceExpr getExpr(V property) {
        return new CaseExpr(where,properties.get(property));
    }

    @Lazy
    public Map<V, SourceExpr> getExprs() {
        Map<V,SourceExpr> result = new HashMap<V, SourceExpr>();
        for(Map.Entry<V,SourceExpr> property : properties.entrySet())
            result.put(property.getKey(),new CaseExpr(where,property.getValue()));
        return result;
    }

    public Where getWhere() {
        return where;
    }

    public Context getContext() {
        return context;
    }

    public Collection<ValueExpr> getValues() {
        return context.values;
    }

    @Lazy
    public <GK extends V, GV extends V> ParsedQuery<GK,GV> groupBy(Collection<GK> keys,Collection<GV> max, Collection<GV> sum) {

        // вытаскиваем Case'ы из keys и max
        Map<GK,KeyExpr> queryKeys = new HashMap<GK, KeyExpr>();
        for(GK queryKey : keys)
            queryKeys.put(queryKey,new KeyExpr(queryKey.toString()));
        JoinQuery<GK,GV> query = new JoinQuery<GK,GV>(queryKeys);
        Where queryWhere = Where.FALSE;
        for(GV property : max)
            query.properties.put(property,new CaseExpr());
        Collection<GV> notNullSum = new ArrayList<GV>();// не null'ы
        for(GV property : sum) {
            query.properties.put(property,new CaseExpr());
            if(!properties.get(property).getWhere().isFalse())
                notNullSum.add(property);
        }

        Where upWhere = where.not();
        for(MapCase<V> mapCase : CaseExpr.pullCases(BaseUtils.filterKeys(properties,BaseUtils.<V,GK,GV>merge(keys,max)))) {
            Where groupWhere = mapCase.where.and(upWhere.not());
            while(true) {
                if(groupWhere.isFalse()) break; // если false сразу вываливаемся                

                Map<GK, AndExpr> groupKeys = BaseUtils.filterKeys(mapCase.data, keys);
                Map<GV, AndExpr> groupMax = BaseUtils.filterKeys(mapCase.data, max);
                Map<GV, SourceExpr> groupSum = BaseUtils.filterKeys(properties, notNullSum);

                Collection<InnerJoins.Entry> innerJoins = GroupQuery.inner?GroupQuery.getInnerJoins(groupWhere,groupKeys,groupMax,groupSum):null;
                Where innerWhere = !GroupQuery.inner || innerJoins.size()==1?groupWhere:innerJoins.iterator().next().where; // если один innerJoin то все ок, иначе нужен "полный" where

                DataJoin<GK,GV> join = new GroupQuery<GK, GV>(groupMax,groupSum,groupKeys,innerWhere).joinAnd(query.mapKeys);
                queryWhere = queryWhere.or(join.getWhere());
                for(GV property : max) {
                    SourceExpr expr = join.getExpr(property);
                    SourceExpr prevExpr = query.properties.get(property);
                    if(prevExpr!=null) expr = new CaseExpr(expr.greater(prevExpr), expr, prevExpr);
                    query.properties.put(property,expr);
                }
                for(GV property : notNullSum) {
                    SourceExpr expr = join.getExpr(property);
                    SourceExpr prevExpr = query.properties.get(property);
                    if(prevExpr!=null) expr = expr.sum(prevExpr);
                    query.properties.put(property,expr);
                }

                if(!GroupQuery.inner || innerJoins.size()==1) break;
                groupWhere = groupWhere.and(innerWhere.not()); // важно чтобы where не "повторился"
            }

            upWhere = upWhere.or(mapCase.where);
        }
        query.and(queryWhere);

        return query.parse();
    }
}
