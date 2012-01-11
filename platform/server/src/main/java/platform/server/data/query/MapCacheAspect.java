package platform.server.data.query;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import platform.base.*;
import platform.server.ParamMessage;
import platform.server.caches.*;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.*;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.*;
import platform.server.session.*;
import platform.server.Settings;

import java.util.*;

@Aspect
public class MapCacheAspect {
    private final static Logger logger = Logger.getLogger(MapCacheAspect.class);
    // добавление кэшированного св-ва, дебильный механизм должен был бы быть IdentityLazy, но так как в кэш помещается другой запрос то ему тоже должены быть проставлен кэш поэтому пока в явную
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

        Result<MapTranslate> translator = new Result<MapTranslate>();
        Query.MultiParamsContext<?, ?> multiParams = cache.getMultiParamsContext().mapInner(query.getMultiParamsContext(), true, translator);
        if(multiParams!=null) {
            Query<K,V> mapCache = (Query<K, V>)multiParams.getQuery();
            Map<CV,V> mapProps = BaseUtils.mapValues(query.properties,mapCache.properties);
            return new MapParsedQuery<CK,CV,K,V>((ParsedJoinQuery<K,V>) ((ParseInterface)cache).getParse(),
                    mapProps,BaseUtils.crossValues(query.mapKeys, mapCache.mapKeys),translator.result.mapValues());
        }
        return null;
    }

    final static Map<Integer, Collection<Query>> cacheParse = new HashMap<Integer, Collection<Query>>();
    <K,V> ParsedQuery<K,V> parse(Query<K,V> query,ProceedingJoinPoint thisJoinPoint) throws Throwable {
        ParsedQuery<K,V> parsed = ((ParseInterface) query).getParse();
        if(parsed!=null) return parsed;

        Collection<Query> hashCaches;
        synchronized(cacheParse) {
            int hashQuery = query.getMultiParamsContext().hashInner(true);
            hashCaches = cacheParse.get(hashQuery);
            if(hashCaches==null) {
                hashCaches = new ArrayList<Query>();
                cacheParse.put(hashQuery, hashCaches);
            }
        }
        synchronized(hashCaches) {
            for(Query<?,?> cache : hashCaches) {
                parsed = cacheQuery(cache, query);
                if(parsed !=null) {
                    logger.info("cached");
                    ((ParseInterface) query).setParse(parsed);
                    return parsed;
                }
            }
            logger.info("not cached");
            Result<Query> cache = new Result<Query>();
            parsed = calculateParse(query, thisJoinPoint, cache);

            ((ParseInterface) query).setParse(parsed);
            hashCaches.add(cache.result);
            return parsed;
        }
    }

    private <K,V> ParsedQuery<K,V> calculateParse(Query<K,V> query, ProceedingJoinPoint thisJoinPoint, Result<Query> cacheResult) throws Throwable {
        Map<Value, Value> bigValues = query.getBigValues();
        if(BaseUtils.onlyObjects(query.mapKeys.keySet()) && BaseUtils.onlyObjects(query.properties.keySet()) && bigValues == null) {
            cacheResult.set(query);
            return (ParsedQuery<K, V>) thisJoinPoint.proceed();
        } else { // чтобы не было утечки памяти, "заменяем" компилируемый запрос на объекты, а все большие значения на поменьше
            Map<K,Object> genKeys = BaseUtils.generateObjects(query.mapKeys.keySet());
            Map<V,Object> genProps = BaseUtils.generateObjects(query.properties.keySet());

            Query<Object, Object> cache = new Query<Object, Object>(BaseUtils.crossJoin(genKeys, query.mapKeys), BaseUtils.crossJoin(genProps, query.properties),
                    query.where);

            if(bigValues!=null) // bigvalues - работа с транслированными объектами, а в конце трансляция назад
                cache = (Query<Object, Object>) cache.translateValues(new MapValuesTranslator(bigValues));

            ParsedQuery<Object,Object> parsedCache = (ParsedQuery<Object,Object>) thisJoinPoint.proceed(new Object[]{cache});
            ((ParseInterface) cache).setParse(parsedCache);

            cacheResult.set(cache);
            return new MapParsedQuery<K, V, Object, Object>(parsedCache, genProps, genKeys,
                    bigValues==null ? MapValuesTranslator.noTranslate : new MapValuesTranslator(BaseUtils.reverse(bigValues)));
        }
    }

    @Around("execution(platform.server.data.query.ParsedQuery platform.server.data.query.Query.parse()) && target(query)")
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

    class JoinImplement<K> extends AbstractInnerContext<JoinImplement<K>> {
        final Map<K,? extends Expr> exprs;
        final MapValuesTranslate mapValues; // map context'а values на те которые нужны

        JoinImplement(Map<K, ? extends Expr> exprs,MapValuesTranslate mapValues) {
            this.exprs = exprs;
            this.mapValues = mapValues;
        }

        public QuickSet<KeyExpr> getKeys() {
            return AbstractOuterContext.getOuterKeys(exprs.values());
        }

        public QuickSet<Value> getValues() {
            // нельзя из values так как вообще не его контекст
            return AbstractOuterContext.getOuterValues(exprs.values()).merge(mapValues.getValues());
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(HashContext hashContext) {
            return AbstractSourceJoin.hashOuter(exprs, hashContext) * 31 + mapValues.hash(hashContext.values);
        }

        protected JoinImplement<K> translate(MapTranslate translate) {
            return new JoinImplement<K>(translate.translate(exprs), mapValues.map(translate.mapValues()));
        }

        public boolean equalsInner(JoinImplement<K> object) {
            return BaseUtils.hashEquals(exprs,object.exprs) && BaseUtils.hashEquals(mapValues,object.mapValues);
        }
    }

    private <K,V> Join<V> join(ParsedJoinQuery query, Map<K, ? extends Expr> joinExprs, MapValuesTranslate joinValues, Map<Integer, Map<JoinImplement<K>, Join<V>>> joinCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {
        assert BaseUtils.onlyObjects(joinExprs.keySet());

        JoinImplement<K> joinImplement = new JoinImplement<K>(joinExprs,joinValues);

        Map<JoinImplement<K>,Join<V>> hashCaches;
        synchronized(joinCaches) {
            int hashImplement = joinImplement.hashInner(true);
            hashCaches = joinCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new HashMap<JoinImplement<K>, Join<V>>();
                joinCaches.put(hashImplement, hashCaches);
            }
        }
        synchronized(hashCaches) {
            for(Map.Entry<JoinImplement<K>,Join<V>> cache : hashCaches.entrySet()) {
                MapTranslate translator;
                if((translator = cache.getKey().mapInner(joinImplement, true))!=null) {
                    // здесь не все values нужно докинуть их из контекста (ключи по идее все)
                    logger.info("join cached");
                    return new DirectTranslateJoin<V>(translator,cache.getValue());
                }
            }
            logger.info("join not cached");
            Map<Value, Value> bigValues = joinImplement.getBigValues();
            if(bigValues == null) {
                Join<V> join = (Join<V>) thisJoinPoint.proceed();
                hashCaches.put(joinImplement, join);
                return join;
            } else { // для предотвращения утечки памяти, bigvalues - работа с транслированными объектами, а в конце трансляция назад
                JoinImplement<K> cacheImplement = joinImplement.translateInner(new MapValuesTranslator(bigValues));

                Join<V> join = (Join<V>) thisJoinPoint.proceed(new Object[]{query, cacheImplement.exprs, cacheImplement.mapValues});
                hashCaches.put(cacheImplement, join);

                return new DirectTranslateJoin<V>(new MapValuesTranslator(BaseUtils.reverse(bigValues)), join);
            }
        }
    }



    @Around("execution(platform.server.data.query.Join platform.server.data.query.ParsedJoinQuery.joinExprs(java.util.Map,platform.server.data.translator.MapValuesTranslate)) && target(query) && args(joinExprs,mapValues)")
    public Object callJoin(ProceedingJoinPoint thisJoinPoint, ParsedJoinQuery query, Map joinExprs, MapValuesTranslate mapValues) throws Throwable {
        return join(query, joinExprs,mapValues,((JoinInterface)query).getJoinCache(),thisJoinPoint);
    }

    public interface MapPropertyInterface {
        Map getExprCache();
        Map getJoinExprCache();
        Map getIncChangeCache();
        Map getDataChangesCache();
        Map getUsedChangesCache();
    }
    public static class MapPropertyInterfaceImplement implements MapPropertyInterface {
        Map<Integer,Map<ExprInterfaceImplement,Query>> exprCache = new HashMap<Integer, Map<ExprInterfaceImplement, Query>>();
        public Map getExprCache() { return exprCache; }

        Map<Integer,Map<JoinExprInterfaceImplement,Query>> joinExprCache = new HashMap<Integer, Map<JoinExprInterfaceImplement, Query>>();
        public Map getJoinExprCache() { return joinExprCache; }

        Map<Integer,Map<PropertyChanges,PropertyChange>> incChangeCache = new HashMap<Integer, Map<PropertyChanges,PropertyChange>>();
        public Map getIncChangeCache() { return incChangeCache; }

        Map<Integer,Map<DataChangesInterfaceImplement,DataChangesResult>> dataChangesCache = new HashMap<Integer, Map<DataChangesInterfaceImplement, DataChangesResult>>();
        public Map getDataChangesCache() { return dataChangesCache; }

        Map<Integer,Map<PropertyChanges, PropertyChanges>> usedChangesCache = new HashMap<Integer, Map<PropertyChanges, PropertyChanges>>();
        public Map getUsedChangesCache() { return usedChangesCache; }
    }
    @DeclareParents(value="platform.server.logics.property.Property",defaultImpl= MapPropertyInterfaceImplement.class)
    private MapPropertyInterface mapPropertyInterface;

    private static boolean checkCaches = false;

    public <K extends PropertyInterface> PropertyChanges getUsedChanges(Property<K> property, PropertyChanges implement, Map<Integer, Map<PropertyChanges, PropertyChanges>> usedCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        if(!(property instanceof FunctionProperty) && !(property instanceof UserProperty && ((UserProperty)property).derivedChange!=null)) // если не Function или DataProperty с derived, то нету рекурсии и эффективнее просто вы
            return (PropertyChanges) thisJoinPoint.proceed();

        if(implement.getContextValues().size > Settings.instance.getUsedChangesCacheLimit() || implement.getBigValues() != null)
            return (PropertyChanges) thisJoinPoint.proceed();

        Map<PropertyChanges, PropertyChanges> hashCaches;
        synchronized(usedCaches) {
            int hashImplement = implement.hashValues();
            hashCaches = usedCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new HashMap<PropertyChanges, PropertyChanges>();
                usedCaches.put(hashImplement, hashCaches);
            }
        }

        synchronized(hashCaches) {
            for(Map.Entry<PropertyChanges, PropertyChanges> cache : hashCaches.entrySet()) {
                for(MapValuesTranslate mapValues : new MapValuesIterable(cache.getKey(), implement)) {
                    if(cache.getKey().translateValues(mapValues).equals(implement)) {
                        logger.info("getUsedChanges - cached "+property);
                        return cache.getValue().translate(mapValues);
                    }
                }
            }

            logger.info("getUsedChanges - not cached "+property);
            PropertyChanges usedChanges = (PropertyChanges) thisJoinPoint.proceed();
            hashCaches.put(implement, usedChanges);

            return usedChanges;
        }
    }

    @Around("execution(* platform.server.logics.property.Property.getUsedChanges(platform.server.session.PropertyChanges)) " +
            "&& target(property) && args(changes) " +
            "&& !cflowbelow(execution(* platform.server.logics.property.Property.getUsedChanges(platform.server.session.PropertyChanges))|| execution(* platform.server.logics.property.Property.calculateExpr(java.util.Map,platform.server.session.PropertyChanges,platform.server.data.where.WhereBuilder)))")
    public Object callGetUsedChanges(ProceedingJoinPoint thisJoinPoint, Property property, PropertyChanges changes) throws Throwable {
        return getUsedChanges(property, changes, ((MapPropertyInterface)property).getUsedChangesCache(), thisJoinPoint);
    }

    @Around("execution(* platform.server.logics.property.Property.getUsedChanges(platform.server.session.PropertyChanges)) " +
            "&& target(property) && args(changes) " +
            "&& cflowbelow(execution(* platform.server.logics.property.Property.getUsedChanges(platform.server.session.PropertyChanges)) || execution(* platform.server.logics.property.Property.calculateExpr(java.util.Map,platform.server.session.PropertyChanges,platform.server.data.where.WhereBuilder)))")
    public Object callRecGetUsedChanges(ProceedingJoinPoint thisJoinPoint, Property property, PropertyChanges changes) throws Throwable {
        if(changes.size() > 20)
            return thisJoinPoint.proceed();
        else
            return getUsedChanges(property, changes, ((MapPropertyInterface)property).getUsedChangesCache(), thisJoinPoint);
    }

    // все равно надо делать класс в котором будет :
    // propertyChange и getUsedDataChanges

    static class DataChangesInterfaceImplement<P extends PropertyInterface> extends AbstractInnerContext<DataChangesInterfaceImplement<P>> {
        final PropertyChanges usedChanges;
        final PropertyChange<P> change;
        final boolean where;

        DataChangesInterfaceImplement(Property<P> property, PropertyChange<P> change, PropertyChanges changes, boolean where) {
            usedChanges = property.getUsedDataChanges(changes);
            this.change = change;
            this.where = where;
        }

        public boolean equalsInner(DataChangesInterfaceImplement<P> o) {
            return BaseUtils.hashEquals(change,o.change) && BaseUtils.hashEquals(usedChanges,o.usedChanges) && where == o.where;
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(HashContext hashContext) {
            return 31 * usedChanges.hashValues(hashContext.values) + change.hashInner(hashContext) + (where?1:0);
        }

        public QuickSet<KeyExpr> getKeys() {
            return change.getInnerKeys();
        }

        public QuickSet<Value> getValues() {
            return usedChanges.getContextValues().merge(change.getInnerValues());
        }

        DataChangesInterfaceImplement(DataChangesInterfaceImplement<P> implement, MapTranslate translator) {
            usedChanges = implement.usedChanges.translateValues(translator.mapValues());
            change = implement.change.translateInner(translator);
            this.where = implement.where;
        }

        protected DataChangesInterfaceImplement<P> translate(MapTranslate translator) {
            return new DataChangesInterfaceImplement<P>(this, translator);
        }
    }

    static class DataChangesResult<P extends PropertyInterface> extends TwinImmutableObject implements TranslateValues<DataChangesResult<P>> {
        MapDataChanges<P> changes;
        Where where;

        DataChangesResult(MapDataChanges<P> changes, Where where) {
            this.changes = changes;
            this.where = where;
        }

        public DataChangesResult<P> translateValues(MapValuesTranslate translate) {
            return new DataChangesResult<P>(changes.translateValues(translate), where==null?null:where.translateOuter(translate.mapKeys()));
        }

        public boolean twins(TwinImmutableInterface o) {
            return changes.equals(((DataChangesResult<P>)o).changes) && BaseUtils.nullEquals(where,((DataChangesResult<P>)o).where);
        }

        public int immutableHashCode() {
            return changes.hashCode() * 31 + BaseUtils.nullHash(where);
        }
    }

    public <K extends PropertyInterface> MapDataChanges<K> getDataChanges(Property<K> property, PropertyChange<K> change, WhereBuilder changedWheres, PropertyChanges propChanges, Map<Integer,Map<DataChangesInterfaceImplement,DataChangesResult>> dataChangesCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        DataChangesInterfaceImplement<K> implement = new DataChangesInterfaceImplement<K>(property,change,propChanges,changedWheres!=null);

        Map<DataChangesInterfaceImplement, DataChangesResult> hashCaches;
        synchronized(dataChangesCaches) {
            int hashImplement = implement.hashInner(true);
            hashCaches = dataChangesCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new HashMap<DataChangesInterfaceImplement, DataChangesResult>();
                dataChangesCaches.put(hashImplement, hashCaches);
            }
        }

        synchronized(hashCaches) {
            for(Map.Entry<DataChangesInterfaceImplement,DataChangesResult> cache : hashCaches.entrySet()) {
                MapTranslate translator;
                if((translator=cache.getKey().mapInner(implement, true))!=null) {
                    logger.info("getDataChanges - cached "+property);
                    if(changedWheres!=null) changedWheres.add(cache.getValue().where.translateOuter(translator));
                    return ((DataChangesResult<K>)cache.getValue()).changes.translateValues(translator.mapValues());
                }
            }

            logger.info("getDataChanges - not cached "+property);
            DataChangesResult<K> result = calculateDataChanges(property, change, changedWheres, propChanges, thisJoinPoint);

            cacheNoBig(implement, hashCaches, result);

            if(changedWheres!=null) changedWheres.add(result.where);
            return result.changes;
        }
    }

    private <I extends ValuesContext<I>, R extends TranslateValues<R>> void cacheNoBig(I implement, Map<I, R> hashCaches, R result) {
        Map<Value, Value> bigValues = implement.getBigValues();
        if(bigValues == null) // если нет больших значений просто записываем
            hashCaches.put(implement, result);
        else { // bigvalues - работа со старыми объектами, а сохранение транслированных
            MapValuesTranslator removeBig = new MapValuesTranslator(bigValues);
            hashCaches.put(implement.translateValues(removeBig), result.translateValues(removeBig));
        }
    }

    private <K extends PropertyInterface> DataChangesResult<K> calculateDataChanges(@ParamMessage Property<K> property, PropertyChange<K> change, WhereBuilder changedWheres, PropertyChanges propChanges, ProceedingJoinPoint thisJoinPoint) throws Throwable {
        WhereBuilder cacheWheres = Property.cascadeWhere(changedWheres);
        MapDataChanges<K> changes = (MapDataChanges<K>) thisJoinPoint.proceed(new Object[]{property,change,propChanges,cacheWheres});
        if(Settings.instance.packOnCacheComplexity > 0 && changes.getComplexity() > Settings.instance.packOnCacheComplexity)
            changes = changes.pack(); // пакуем так как в кэш складываем
        return new DataChangesResult<K>(changes, changedWheres != null ? cacheWheres.toWhere() : null);
    }

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("execution(platform.server.session.MapDataChanges platform.server.logics.property.Property.getDataChanges(platform.server.session.PropertyChange,platform.server.session.PropertyChanges,platform.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(change,propChanges,changedWhere)")
    public Object callGetDataChanges(ProceedingJoinPoint thisJoinPoint, Property property, PropertyChange change, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        // сначала target в аспекте должен быть
        return getDataChanges(property, change, changedWhere, propChanges, ((MapPropertyInterface)property).getDataChangesCache(), thisJoinPoint);
    }

    static class JoinExprInterfaceImplement<K extends PropertyInterface> extends AbstractInnerContext<JoinExprInterfaceImplement<K>> {
        final PropertyChanges usedChanges;
        final boolean changed; // нужно ли условие на изменение, по сути для этого св-ва и делается класс
        final Map<K, ? extends Expr> values;

        JoinExprInterfaceImplement(Property<?> property, PropertyChanges changes, boolean changed, Map<K, ? extends Expr> values) {
            usedChanges = property.getUsedChanges(changes);
            this.changed = changed;
            this.values = values;
        }

        protected QuickSet<KeyExpr> getKeys() {
            return QuickSet.EMPTY();
        }

        public boolean equalsInner(JoinExprInterfaceImplement<K> o) {
            return changed == o.changed && usedChanges.equals(o.usedChanges) && values.equals(o.values);
        }

        protected boolean isComplex() {
            return true;
        }
        protected int hash(HashContext hashContext) {
            int hash = 0;
            for(Map.Entry<K, ? extends Expr> value : values.entrySet())
                hash += value.getKey().hashCode() ^ value.getValue().hashOuter(hashContext);
            return 31 * (usedChanges.hashValues(hashContext.values) * 31 + hash) + (changed ? 1 : 0);
        }

        public QuickSet<Value> getValues() {
            return AbstractOuterContext.getOuterValues(values.values()).merge(usedChanges.getContextValues());
        }

        JoinExprInterfaceImplement(JoinExprInterfaceImplement<K> implement, MapTranslate translator) {
            usedChanges = implement.usedChanges.translateValues(translator.mapValues());
            this.changed = implement.changed;
            this.values = translator.translate(implement.values);  // assert что keys'ов нет
        }

        protected JoinExprInterfaceImplement<K> translate(MapTranslate translator) {
            return new JoinExprInterfaceImplement<K>(this, translator);
        }
    }

    public <K extends PropertyInterface> Query<K, String> getQuery(Property<K> property, PropertyChanges propChanges, boolean changedWhere, Map<K, ? extends Expr> interfaceValues, Map<Integer, Map<JoinExprInterfaceImplement<K>, Query<K, String>>> exprCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {
        // assert что в interfaceValues только values

        JoinExprInterfaceImplement<K> implement = new JoinExprInterfaceImplement<K>(property,propChanges,changedWhere,interfaceValues);

        Map<JoinExprInterfaceImplement<K>,Query<K, String>> hashCaches;
        synchronized(exprCaches) {
            int hashImplement = implement.hashValues();
            hashCaches = exprCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new HashMap<JoinExprInterfaceImplement<K>, Query<K, String>>();
                exprCaches.put(hashImplement, hashCaches);
            }
        }

        Query<K,String> query, cacheQuery = null;
        synchronized(hashCaches) {
            for(Map.Entry<JoinExprInterfaceImplement<K>, Query<K, String>> cache : hashCaches.entrySet()) {
                for(MapValuesTranslate mapValues : new MapValuesIterable(cache.getKey(), implement)) {
                    if(cache.getKey().translateValues(mapValues).equals(implement)) {
                        cacheQuery = cache.getValue().translateValues(mapValues.filter(cache.getValue().getInnerValues())); // так как могут не использоваться values в Query
                        break;
                    }
                }
            }
            if(cacheQuery==null || checkCaches) {
                query = (Query<K, String>) thisJoinPoint.proceed();

                assert implement.getContextValues().getSet().containsAll(ValueExpr.removeStatic(query.getInnerValues().getSet())); // в query не должно быть элементов не из implement.getContextValues

                if(!(checkCaches && cacheQuery!=null))
                    cacheNoBig(implement, hashCaches, query);

                logger.info("getExpr - not cached "+property);
            } else {
                query = cacheQuery;

                logger.info("getExpr - cached "+property);
            }
        }

        if (checkCaches && cacheQuery!=null && !BaseUtils.hashEquals(query, cacheQuery))
            query = query;
        return query;
    }

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("execution(* platform.server.logics.property.Property.getQuery(platform.server.session.PropertyChanges,java.lang.Boolean,java.util.Map)) " +
            "&& target(property) && args(propChanges,changedWhere,interfaceValues)")
    public Object callGetQuery(ProceedingJoinPoint thisJoinPoint, Property property, PropertyChanges propChanges, Boolean changedWhere, Map interfaceValues) throws Throwable {
        // сначала target в аспекте должен быть
        return getQuery(property, propChanges, changedWhere, interfaceValues, ((MapPropertyInterface) property).getJoinExprCache(), thisJoinPoint);
    }

    static class ExprInterfaceImplement<P extends PropertyInterface> extends AbstractInnerContext<ExprInterfaceImplement<P>> {
        final PropertyChanges usedChanges;
        final Map<P, Expr> joinImplement;
        final boolean where;

        ExprInterfaceImplement(Property<P> property, Map<P,Expr> joinImplement, PropertyChanges propChanges, boolean where) {
            usedChanges = property.getUsedChanges(propChanges);
            this.joinImplement = joinImplement;
            this.where = where;
        }

        public boolean equalsInner(ExprInterfaceImplement<P> o) {
            return BaseUtils.hashEquals(joinImplement,o.joinImplement) && BaseUtils.hashEquals(usedChanges,o.usedChanges) && where == o.where;
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(HashContext hashContext) {
            return 31 * usedChanges.hashValues(hashContext.values) + AbstractOuterContext.hashOuter(joinImplement, hashContext) + (where?1:0);
        }

        public QuickSet<KeyExpr> getKeys() {
            return AbstractOuterContext.getOuterKeys(joinImplement.values());
        }

        public QuickSet<Value> getValues() {
            return AbstractOuterContext.getOuterValues(joinImplement.values()).merge(usedChanges.getContextValues());
        }

        ExprInterfaceImplement(ExprInterfaceImplement<P> implement, MapTranslate translator) {
            usedChanges = implement.usedChanges.translate(translator.mapValues());
            joinImplement = translator.translate(implement.joinImplement);
            this.where = implement.where;
        }

        public ExprInterfaceImplement<P> translate(MapTranslate translator) {
            return new ExprInterfaceImplement<P>(this, translator);
        }
    }

    static class ExprResult extends TwinImmutableObject implements TranslateValues<ExprResult> {
        Expr expr;
        Where where;

        ExprResult(Expr expr, Where where) {
            this.expr = expr;
            this.where = where;
        }

        public ExprResult translateValues(MapValuesTranslate translate) {
            return new ExprResult(expr.translateOuter(translate.mapKeys()), where == null ? null : where.translateOuter(translate.mapKeys()));
        }

        public boolean twins(TwinImmutableInterface o) {
            return expr.equals(((ExprResult)o).expr) && BaseUtils.nullEquals(where, ((ExprResult)o).where);
        }

        public int immutableHashCode() {
            return expr.hashCode() * 31 + BaseUtils.nullHash(where);
        }
    }

    public static boolean disableCaches = false;

    public <K extends PropertyInterface> Expr getJoinExpr(Property<K> property, Map<K, Expr> joinExprs, PropertyChanges propChanges, WhereBuilder changedWheres, Map<Integer, Map<ExprInterfaceImplement, ExprResult>> exprCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        // здесь по идее на And не надо проверять
        if(disableCaches)
            return (Expr) thisJoinPoint.proceed();

        property.cached = true;

        ExprInterfaceImplement<K> implement = new ExprInterfaceImplement<K>(property,joinExprs,propChanges,changedWheres!=null);

        Map<ExprInterfaceImplement, ExprResult> hashCaches;
        synchronized(exprCaches) {
            int hashImplement = implement.hashInner(true);
            hashCaches = exprCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new HashMap<ExprInterfaceImplement, ExprResult>();
                exprCaches.put(hashImplement, hashCaches);
            }
        }

        synchronized(hashCaches) {
            Expr cacheResult = null;
            for(Map.Entry<ExprInterfaceImplement,ExprResult> cache : hashCaches.entrySet()) {
                MapTranslate translator;
                if((translator=cache.getKey().mapInner(implement, true))!=null) {
                    logger.info("getExpr - cached "+property);
                    if(changedWheres!=null) changedWheres.add(cache.getValue().where.translateOuter(translator));
                    cacheResult = cache.getValue().expr.translateOuter(translator);
                    if(checkCaches)
                        break;
                    else
                        return cacheResult;
                }
            }

            logger.info("getExpr - not cached "+property);
            WhereBuilder cacheWheres = Property.cascadeWhere(changedWheres);
            Expr expr = (Expr) thisJoinPoint.proceed(new Object[]{property, joinExprs, propChanges, cacheWheres});

            if(Settings.instance.packOnCacheComplexity > 0 && expr.getComplexity(false) > Settings.instance.packOnCacheComplexity)
                expr = expr.pack(); // пакуем так как в кэш идет

            cacheNoBig(implement, hashCaches, new ExprResult(expr, changedWheres != null ? cacheWheres.toWhere() : null));
            if(checkCaches && !BaseUtils.hashEquals(expr, cacheResult))
                expr = expr;

            // проверим
            if(checkInfinite && !(property instanceof FormulaProperty))
                expr.checkInfiniteKeys();

            if(changedWheres!=null) changedWheres.add(cacheWheres.toWhere());
            return expr;
        }
    }

    public static boolean checkInfinite = false;

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("execution(* platform.server.logics.property.Property.getJoinExpr(java.util.Map,platform.server.session.PropertyChanges,platform.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,propChanges,changedWhere)")
    public Object callGetJoinExpr(ProceedingJoinPoint thisJoinPoint, Property property, Map joinExprs, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        // сначала target в аспекте должен быть
        return getJoinExpr(property, joinExprs, propChanges, changedWhere, ((MapPropertyInterface) property).getExprCache(), thisJoinPoint);
    }

    public <K extends PropertyInterface> PropertyChange<K> getIncrementChange(Property<K> property, PropertyChanges propChanges, Map<Integer, Map<PropertyChanges, PropertyChange<K>>> exprCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {
        // assert что в interfaceValues только values

        PropertyChanges implement = property.getUsedChanges(propChanges);

        Map<PropertyChanges, PropertyChange<K>> hashCaches;
        synchronized(exprCaches) {
            int hashImplement = implement.hashValues();
            hashCaches = exprCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new HashMap<PropertyChanges, PropertyChange<K>>();
                exprCaches.put(hashImplement, hashCaches);
            }
        }

        PropertyChange<K> change;
        PropertyChange<K> cacheChange = null;
        synchronized(hashCaches) {
            for(Map.Entry<PropertyChanges, PropertyChange<K>> cache : hashCaches.entrySet()) {
                for(MapValuesTranslate mapValues : new MapValuesIterable(cache.getKey(), implement)) {
                    if(cache.getKey().translateValues(mapValues).equals(implement)) {
                        cacheChange = cache.getValue().translateValues(mapValues.filter(cache.getValue().getInnerValues())); // так как могут не использоваться values в Query
                        break;
                    }
                }
            }
            if(cacheChange==null || checkCaches) {
                change = (PropertyChange<K>) thisJoinPoint.proceed();

                assert implement.getContextValues().getSet().containsAll(ValueExpr.removeStatic(change.getInnerValues().getSet())); // в query не должно быть элементов не из implement.getContextValues

                if(!(checkCaches && cacheChange!=null))
                    cacheNoBig(implement, hashCaches, change);

                logger.info("getIncrementChange - not cached "+property);
            } else {
                change = cacheChange;

                logger.info("getIncrementChange - cached "+property);
            }
        }

        if (checkCaches && cacheChange!=null && !BaseUtils.hashEquals(change, cacheChange))
            change = change;
        return change;
    }

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("execution(* platform.server.logics.property.Property.getIncrementChange(platform.server.session.PropertyChanges)) " +
            "&& target(property) && args(propChanges)")
    public Object callGetIncrementChange(ProceedingJoinPoint thisJoinPoint, Property property, PropertyChanges propChanges) throws Throwable {
        return getIncrementChange(property, propChanges, ((MapPropertyInterface) property).getIncChangeCache(), thisJoinPoint);
    }
}
