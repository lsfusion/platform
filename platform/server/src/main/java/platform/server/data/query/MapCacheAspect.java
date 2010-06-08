package platform.server.data.query;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import platform.base.BaseUtils;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashValues;
import platform.server.caches.hash.HashMapValues;
import platform.server.caches.AbstractMapValues;
import platform.server.caches.GenericImmutable;
import platform.server.caches.GenericLazy;
import platform.server.caches.MapContext;
import platform.server.caches.MapHashIterable;
import platform.server.caches.MapParamsIterable;
import platform.server.caches.MapValuesIterable;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.DirectTranslator;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.AndFormulaProperty;
import platform.server.logics.property.AggregateProperty;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;

import java.util.*;

@Aspect
public class MapCacheAspect {

    // добавление кэшированного св-ва
    public interface ParseInterface {
        ParsedQuery getParse(); void setParse(ParsedQuery query);
    }
    public static class ParseInterfaceImplement implements ParseInterface {
        ParsedQuery parse = null;
        public ParsedQuery getParse() { return parse; } public void setParse(ParsedQuery query) {parse = query;}
    }
    @DeclareParents(value="platform.server.data.query.Query",defaultImpl=ParseInterfaceImplement.class)
    private ParseInterface parseInterface;

    static <K,V,CK,CV> MapParsedQuery<CK,CV,K,V> cacheQuery(Query<K,V> cache, Query<CK,CV> query) {
        for(DirectTranslator translator : new MapHashIterable(cache, query, true)) {
            Map<CV,V> mapProps;
            if(cache.where.translateDirect(translator).equals(query.where) && (mapProps=BaseUtils.mapEquals(query.properties,translator.translate(cache.properties)))!=null)
                return new MapParsedQuery<CK,CV,K,V>((ParsedJoinQuery<K,V>) ((ParseInterface)cache).getParse(),
                        mapProps,BaseUtils.crossValues(query.mapKeys, cache.mapKeys, translator.keys),translator.values);
        }
        return null;
    }

    final static Map<Integer, Collection<Query>> cacheParse = new HashMap<Integer, Collection<Query>>();
    <K,V> ParsedQuery<K,V> parse(Query<K,V> query,ProceedingJoinPoint thisJoinPoint) throws Throwable {
        ParsedQuery<K,V> parsed = ((ParseInterface) query).getParse();
        if(parsed!=null) return parsed;

        Collection<Query> hashCaches;
        synchronized(cacheParse) {
            int hashQuery = MapParamsIterable.hash(query,true);
            hashCaches = cacheParse.get(hashQuery);
            if(hashCaches==null) {
                hashCaches = new ArrayList<Query>();
                cacheParse.put(hashQuery, hashCaches);
            }
        }
//        synchronized(hashCaches) {
            for(Query<?,?> cache : hashCaches) {
                parsed = cacheQuery(cache, query);
                if(parsed !=null) {
                    System.out.println("cached");
                    return parsed;
                }
            }
            System.out.println("not cached");
            parsed = (ParsedQuery<K, V>) thisJoinPoint.proceed();
            ((ParseInterface) query).setParse(parsed);
            hashCaches.add(query);
            return parsed;
//        }
    }

    @Around("call(platform.server.data.query.ParsedQuery platform.server.data.query.Query.parse()) && target(query)")
    public Object callParse(ProceedingJoinPoint thisJoinPoint, Query query) throws Throwable {
        return parse(query,thisJoinPoint);
    }

    public interface JoinInterface {
        Map getJoinCache();
    }
    public static class JoinInterfaceImplement implements JoinInterface {
        Map<Integer,Map<JoinImplement,Join>> joinCache = new HashMap<Integer, Map<JoinImplement, Join>>();
        public Map getJoinCache() { return joinCache; }
    }
    @DeclareParents(value="platform.server.data.query.ParsedJoinQuery",defaultImpl=JoinInterfaceImplement.class)
    private JoinInterface joinInterface;

    @GenericImmutable
    class JoinImplement<K> implements MapContext {
        final Map<K,? extends Expr> exprs;
        final Map<ValueExpr,ValueExpr> mapValues; // map context'а values на те которые нужны

        JoinImplement(Map<K, ? extends Expr> exprs,Map<ValueExpr,ValueExpr> mapValues) {
            this.exprs = exprs;
            this.mapValues = mapValues;
        }

        @GenericLazy
        public Set<KeyExpr> getKeys() {
            return AbstractSourceJoin.enumKeys(exprs.values());
        }

        @GenericLazy
        public Set<ValueExpr> getValues() {
            // нельзя из values так как вообще не его контекст
            return AbstractSourceJoin.enumValues(exprs.values());
        }

        @GenericLazy
        public int hash(HashContext hashContext) {
            int hash=0;
            for(Map.Entry<K,? extends Expr> expr : exprs.entrySet())
                hash += expr.getKey().hashCode() ^ expr.getValue().hashContext(hashContext);
            return hash;
        }
    }

    <K,V> Join<V> join(Map<K,? extends Expr> joinExprs,Map<ValueExpr, ValueExpr> joinValues,Map<Integer,Map<JoinImplement<K>,Join<V>>> joinCaches,ProceedingJoinPoint thisJoinPoint) throws Throwable {
        JoinImplement<K> joinImplement = new JoinImplement<K>(joinExprs,joinValues);

        Map<JoinImplement<K>,Join<V>> hashCaches;
        synchronized(joinCaches) {
            int hashImplement = MapParamsIterable.hash(joinImplement,true);
            hashCaches = joinCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new HashMap<JoinImplement<K>, Join<V>>();
                joinCaches.put(hashImplement, hashCaches);
            }
        }
        synchronized(hashCaches) {
            for(Map.Entry<JoinImplement<K>,Join<V>> cache : hashCaches.entrySet())
                for(DirectTranslator translator : new MapHashIterable(cache.getKey(), joinImplement, true))
                    if(translator.translate(cache.getKey().exprs).equals(joinImplement.exprs)) {
                        // здесь не все values нужно докинуть их из контекста (ключи по идее все)
                        Map<ValueExpr,ValueExpr> transValues;
                        if((transValues=BaseUtils.mergeEqual(translator.values,BaseUtils.crossJoin(cache.getKey().mapValues,joinImplement.mapValues)))!=null) {
                            System.out.println("join cached");
                            return new DirectTranslateJoin<V>(new DirectTranslator(translator.keys,transValues),cache.getValue());
                        }
                    }
            System.out.println("join not cached");
            Join<V> join = (Join<V>) thisJoinPoint.proceed();
            hashCaches.put(joinImplement,join);
            return join;
        }
    }

    @Around("call(platform.server.data.query.Join platform.server.data.query.ParsedJoinQuery.joinExprs(java.util.Map,java.util.Map)) && target(query) && args(joinExprs,joinValues)")
    public Object callJoin(ProceedingJoinPoint thisJoinPoint, ParsedJoinQuery query, Map joinExprs, Map joinValues) throws Throwable {
        return join(joinExprs,joinValues,((JoinInterface)query).getJoinCache(),thisJoinPoint);
    }

    public interface MapPropertyInterface {
        Map getExprCache();
        Map getJoinExprCache();
        Map getDataChangesCache();
        Map getUsedChangesCache();
    }
    public static class MapPropertyInterfaceImplement implements MapPropertyInterface {
        Map<Integer,Map<ExprInterfaceImplement,Query>> exprCache = new HashMap<Integer, Map<ExprInterfaceImplement, Query>>();
        public Map getExprCache() { return exprCache; }

        Map<Integer,Map<JoinExprInterfaceImplement,Query>> joinExprCache = new HashMap<Integer, Map<JoinExprInterfaceImplement, Query>>();
        public Map getJoinExprCache() { return joinExprCache; }

        Map<Integer,Map<DataChangesInterfaceImplement,DataChangesResult>> dataChangesCache = new HashMap<Integer, Map<DataChangesInterfaceImplement, DataChangesResult>>();
        public Map getDataChangesCache() { return dataChangesCache; }

        Map<Integer,Map<Changes,Changes>> usedChangesCache = new HashMap<Integer, Map<Changes, Changes>>();
        public Map getUsedChangesCache() { return usedChangesCache; }
    }
    @DeclareParents(value="platform.server.logics.property.Property",defaultImpl= MapPropertyInterfaceImplement.class)
    private MapPropertyInterface mapPropertyInterface;

    private static boolean checkCaches = false;

    public <K extends PropertyInterface,U extends Changes<U>> U getUsedChanges(Property<K> property, Modifier<U> modifier, Map<Integer, Map<U, U>> usedCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        U implement = modifier.fullChanges();

        Map<U, U> hashCaches;
        synchronized(usedCaches) {
            int hashImplement = implement.hashValues(HashMapValues.instance);
            hashCaches = usedCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new HashMap<U, U>();
                usedCaches.put(hashImplement, hashCaches);
            }
        }

        synchronized(hashCaches) {
            for(Map.Entry<U,U> cache : hashCaches.entrySet()) {
                for(Map<ValueExpr,ValueExpr> mapValues : new MapValuesIterable(cache.getKey(), implement)) {
                    if(cache.getKey().translate(mapValues).equals(implement)) {
                        System.out.println("getUsedChanges - cached "+property);
                        return cache.getValue().translate(mapValues);
                    }
                }
            }

            U usedChanges = (U) thisJoinPoint.proceed();
            hashCaches.put(implement, usedChanges);
            System.out.println("getUsedChanges - not cached "+property);

            return usedChanges;
        }
    }

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("call(* platform.server.logics.property.Property.aspectGetUsedChanges(platform.server.session.Modifier)) " +
            "&& target(property) && args(modifier)")
    public Object callGetUsedChanges(ProceedingJoinPoint thisJoinPoint, Property property, Modifier modifier) throws Throwable {
        // сначала target в аспекте должен быть
        return getUsedChanges(property, modifier, ((MapPropertyInterface)property).getUsedChangesCache(), thisJoinPoint);
    }

    final String PROPERTY_STRING = "expr";
    final String CHANGED_STRING = "where";

    @GenericImmutable
    static class JoinExprInterfaceImplement<U extends Changes<U>> extends AbstractMapValues<JoinExprInterfaceImplement<U>> {
        final U usedChanges;
        final boolean changed; // нужно ли условие на изменение, по сути для этого св-ва и делается класс

        JoinExprInterfaceImplement(Property<?> property, Modifier<U> modifier, boolean changed) {
            usedChanges = property.getUsedChanges(modifier);
            this.changed = changed;
        }

        public boolean equals(Object o) {
            return this == o || o instanceof JoinExprInterfaceImplement && changed == ((JoinExprInterfaceImplement) o).changed && usedChanges.equals(((JoinExprInterfaceImplement) o).usedChanges);
        }

        @GenericLazy
        public int hashValues(HashValues hashValues) {
            return 31 * usedChanges.hashValues(hashValues) + (changed ? 1 : 0);
        }

        public Set<ValueExpr> getValues() {
            return usedChanges.getValues();
        }

        JoinExprInterfaceImplement(JoinExprInterfaceImplement<U> implement, Map<ValueExpr,ValueExpr> mapValues) {
            usedChanges = implement.usedChanges.translate(mapValues);
            this.changed = implement.changed;
        }

        public JoinExprInterfaceImplement<U> translate(Map<ValueExpr,ValueExpr> mapValues) {
            return new JoinExprInterfaceImplement<U>(this, mapValues);
        }
    }

    public <K extends PropertyInterface,U extends Changes<U>> Expr getJoinExpr(Property<K> property, Map<K, Expr> joinExprs, Modifier<U> modifier, WhereBuilder changedWheres, Map<Integer,Map<JoinExprInterfaceImplement<U>,Query<K,String>>> exprCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        // если свойство AndFormulaProperty - то есть нарушается инвариант что все входные не null идет autoFillDB то не кэшируем
        if(property instanceof AndFormulaProperty || property.equals(AggregateProperty.recalculate)) return (Expr) thisJoinPoint.proceed();

        property.cached = true;

        JoinExprInterfaceImplement<U> implement = new JoinExprInterfaceImplement<U>(property,modifier,changedWheres!=null);

        Map<JoinExprInterfaceImplement<U>,Query<K,String>> hashCaches;
        synchronized(exprCaches) {
            int hashImplement = implement.hashValues(HashMapValues.instance);
            hashCaches = exprCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new HashMap<JoinExprInterfaceImplement<U>, Query<K, String>>();
                exprCaches.put(hashImplement, hashCaches);
            }
        }

        boolean cached = false;

        Join<String> queryJoin = null;
        synchronized(hashCaches) {
            for(Map.Entry<JoinExprInterfaceImplement<U>,Query<K,String>> cache : hashCaches.entrySet()) {
                for(Map<ValueExpr,ValueExpr> mapValues : new MapValuesIterable(cache.getKey(), implement)) {
                    if(cache.getKey().translate(mapValues).equals(implement)) {
                        cached = true;
                        queryJoin = cache.getValue().join(joinExprs,BaseUtils.filterKeys(mapValues,cache.getValue().getValues())); // так как могут не использоваться values в Query
                    }
                }
            }
            if(queryJoin==null) {
                // надо проверить что с такими changes, defaultProps, noUpdateProps
                Query<K,String> query = new Query<K,String>(property);
                WhereBuilder queryWheres = Property.cascadeWhere(changedWheres);
                query.properties.put(PROPERTY_STRING, (Expr) thisJoinPoint.proceed(new Object[]{property,property,query.mapKeys,modifier,queryWheres}));
                if(changedWheres!=null)
                    query.properties.put(CHANGED_STRING,ValueExpr.get(queryWheres.toWhere()));

                hashCaches.put(implement, query);
                assert implement.getValues().containsAll(ValueExpr.removeStatic(query.getValues())); // в query не должно быть элементов не из implement.getValues

                System.out.println("getExpr - not cached "+property);
                queryJoin = query.join(joinExprs);
            }
        }

        if(changedWheres!=null) changedWheres.add(queryJoin.getExpr(CHANGED_STRING).getWhere());

        Expr result = queryJoin.getExpr(PROPERTY_STRING);
        if(cached) {
            System.out.println("getExpr - cached "+property);
            if(checkCaches) {
                WhereBuilder queryWheres = Property.cascadeWhere(changedWheres);
                Expr notCachedResult = (Expr) thisJoinPoint.proceed(new Object[]{property,property,joinExprs,modifier,queryWheres});
                if(!notCachedResult.equals(result))
                    result = result;
            }
        }
        return result;
    }

    // все равно надо делать класс в котором будет :
    // propertyChange и getUsedDataChanges

    @GenericImmutable
    static class DataChangesInterfaceImplement<P extends PropertyInterface, U extends Changes<U>> implements MapContext {
        final U usedChanges;
        final PropertyChange<P> change;
        final boolean where;

        DataChangesInterfaceImplement(Property<P> property, PropertyChange<P> change, Modifier<U> modifier, boolean where) {
            usedChanges = property.getUsedDataChanges(modifier);
            this.change = change;
            this.where = where;
        }

        public boolean equals(Object o) {
            return this == o || o instanceof DataChangesInterfaceImplement && change.equals(((DataChangesInterfaceImplement) o).change) &&
                    usedChanges.equals(((DataChangesInterfaceImplement) o).usedChanges) && where == ((DataChangesInterfaceImplement) o).where;
        }

        @GenericLazy
        public int hash(HashContext hashContext) {
            return 31 * usedChanges.hashValues(hashContext) + change.hash(hashContext) + (where?1:0);
        }

        public Set<KeyExpr> getKeys() {
            return change.getKeys();
        }

        public Set<ValueExpr> getValues() {
            Set<ValueExpr> result = new HashSet<ValueExpr>();
            result.addAll(usedChanges.getValues());
            result.addAll(change.getValues());
            return result;
        }

        DataChangesInterfaceImplement(DataChangesInterfaceImplement<P,U> implement, DirectTranslator translator) {
            usedChanges = implement.usedChanges.translate(translator.values);
            change = implement.change.translate(translator);
            this.where = implement.where;
        }

        public DataChangesInterfaceImplement<P,U> translate(DirectTranslator translator) {
            return new DataChangesInterfaceImplement<P,U>(this, translator);
        }
    }

    static class DataChangesResult<P extends PropertyInterface> {
        MapDataChanges<P> changes;
        Where where;

        DataChangesResult(MapDataChanges<P> changes, Where where) {
            this.changes = changes;
            this.where = where;
        }
    }

    public <K extends PropertyInterface,U extends Changes<U>> MapDataChanges<K> getDataChanges(Property<K> property, PropertyChange<K> change, WhereBuilder changedWheres, Modifier<U> modifier, Map<Integer,Map<DataChangesInterfaceImplement,DataChangesResult>> dataChangesCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        DataChangesInterfaceImplement<K,U> implement = new DataChangesInterfaceImplement<K,U>(property,change,modifier,changedWheres!=null);

        Map<DataChangesInterfaceImplement, DataChangesResult> hashCaches;
        synchronized(dataChangesCaches) {
            int hashImplement = MapParamsIterable.hash(implement,true);
            hashCaches = dataChangesCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new HashMap<DataChangesInterfaceImplement, DataChangesResult>();
                dataChangesCaches.put(hashImplement, hashCaches);
            }
        }

        synchronized(hashCaches) {
            for(Map.Entry<DataChangesInterfaceImplement,DataChangesResult> cache : hashCaches.entrySet()) {
                for(DirectTranslator translator : new MapHashIterable(cache.getKey(), implement, true)) {
                    if(cache.getKey().translate(translator).equals(implement)) {
                        System.out.println("getDataChanges - cached "+property);
                        if(changedWheres!=null) changedWheres.add(cache.getValue().where.translateDirect(translator));
                        return ((DataChangesResult<K>)cache.getValue()).changes.translate(translator.values);
                    }
                }
            }

            WhereBuilder cacheWheres = Property.cascadeWhere(changedWheres);
            MapDataChanges<K> changes = (MapDataChanges<K>) thisJoinPoint.proceed(new Object[]{property,property,change,cacheWheres,modifier});
            hashCaches.put(implement, new DataChangesResult<K>(changes, changedWheres!=null?cacheWheres.toWhere():null));
            System.out.println("getDataChanges - not cached "+property);

            if(changedWheres!=null) changedWheres.add(cacheWheres.toWhere());
            return changes;
        }
    }

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("call(platform.server.session.MapDataChanges platform.server.logics.property.Property.getDataChanges(platform.server.session.PropertyChange,platform.server.data.where.WhereBuilder,platform.server.session.Modifier)) " +
            "&& target(property) && args(change,changedWhere,modifier)")
    public Object callGetDataChanges(ProceedingJoinPoint thisJoinPoint, Property property, PropertyChange change, WhereBuilder changedWhere, Modifier modifier) throws Throwable {
        // сначала target в аспекте должен быть
        return getDataChanges(property, change, changedWhere, modifier, ((MapPropertyInterface)property).getDataChangesCache(), thisJoinPoint);
    }


    @GenericImmutable
    static class ExprInterfaceImplement<P extends PropertyInterface, U extends Changes<U>> implements MapContext {
        final U usedChanges;
        final Map<P, Expr> joinImplement;
        final boolean where;

        ExprInterfaceImplement(Property<P> property, Map<P,Expr> joinImplement, Modifier<U> modifier, boolean where) {
            usedChanges = property.getUsedChanges(modifier);
            this.joinImplement = joinImplement;
            this.where = where;
        }

        public boolean equals(Object o) {
            return this == o || o instanceof ExprInterfaceImplement && joinImplement.equals(((ExprInterfaceImplement) o).joinImplement) &&
                    usedChanges.equals(((ExprInterfaceImplement) o).usedChanges) && where == ((ExprInterfaceImplement) o).where;
        }

        @GenericLazy
        public int hash(HashContext hashContext) {
            int hash = 0;
            for(Map.Entry<P,Expr> joinExpr : joinImplement.entrySet())
                hash += joinExpr.getKey().hashCode() ^ joinExpr.getValue().hashContext(hashContext);
            return 31 * usedChanges.hashValues(hashContext) + hash + (where?1:0);
        }

        public Set<KeyExpr> getKeys() {
            return AbstractSourceJoin.enumKeys(joinImplement.values());
        }

        public Set<ValueExpr> getValues() {
            Set<ValueExpr> result = new HashSet<ValueExpr>();
            result.addAll(usedChanges.getValues());
            result.addAll(AbstractSourceJoin.enumValues(joinImplement.values()));
            return result;
        }

        ExprInterfaceImplement(ExprInterfaceImplement<P,U> implement, DirectTranslator translator) {
            usedChanges = implement.usedChanges.translate(translator.values);
            joinImplement = translator.translate(implement.joinImplement);
            this.where = implement.where;
        }

        public ExprInterfaceImplement<P,U> translate(DirectTranslator translator) {
            return new ExprInterfaceImplement<P,U>(this, translator);
        }
    }

    static class ExprResult {
        Expr expr;
        Where where;

        ExprResult(Expr expr, Where where) {
            this.expr = expr;
            this.where = where;
        }
    }

    public <K extends PropertyInterface,U extends Changes<U>> Expr getExpr(Property<K> property, Map<K, Expr> joinExprs, Modifier<U> modifier, WhereBuilder changedWheres, Map<Integer, Map<ExprInterfaceImplement, ExprResult>> exprCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        // здесь по идее на And не надо проверять
        if(property.equals(AggregateProperty.recalculate)) return (Expr) thisJoinPoint.proceed();

        ExprInterfaceImplement<K,U> implement = new ExprInterfaceImplement<K,U>(property,joinExprs,modifier,changedWheres!=null);

        Map<ExprInterfaceImplement, ExprResult> hashCaches;
        synchronized(exprCaches) {
            int hashImplement = MapParamsIterable.hash(implement,true);
            hashCaches = exprCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new HashMap<ExprInterfaceImplement, ExprResult>();
                exprCaches.put(hashImplement, hashCaches);
            }
        }

        synchronized(hashCaches) {
            for(Map.Entry<ExprInterfaceImplement,ExprResult> cache : hashCaches.entrySet()) {
                for(DirectTranslator translator : new MapHashIterable(cache.getKey(), implement, true)) {
                    if(cache.getKey().translate(translator).equals(implement)) {
                        System.out.println("getExpr - cached "+property);
                        if(changedWheres!=null) changedWheres.add(cache.getValue().where.translateDirect(translator));
                        return cache.getValue().expr.translateDirect(translator);
                    }
                }
            }

            WhereBuilder cacheWheres = Property.cascadeWhere(changedWheres);
            Expr expr = (Expr) thisJoinPoint.proceed(new Object[]{property,property,joinExprs,modifier,cacheWheres});
            hashCaches.put(implement, new ExprResult(expr, changedWheres!=null?cacheWheres.toWhere():null));
            System.out.println("getExpr - not cached "+property);

            if(changedWheres!=null) changedWheres.add(cacheWheres.toWhere());
            return expr;
        }
    }

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("call(platform.server.data.expr.Expr platform.server.logics.property.Property.getExpr(java.util.Map,platform.server.session.Modifier,platform.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,modifier,changedWhere)")
    public Object callGetExpr(ProceedingJoinPoint thisJoinPoint, Property property, Map joinExprs, Modifier modifier,WhereBuilder changedWhere) throws Throwable {
        // сначала target в аспекте должен быть
//        return getJoinExpr(property, joinExprs, modifier, changedWhere, ((MapPropertyInterface)property).getJoinExprCache(), thisJoinPoint);
        return getExpr(property, joinExprs, modifier, changedWhere, ((MapPropertyInterface)property).getExprCache(), thisJoinPoint);
    }
}
