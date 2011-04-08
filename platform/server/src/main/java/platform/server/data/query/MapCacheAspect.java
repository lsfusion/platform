package platform.server.data.query;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import platform.base.BaseUtils;
import platform.base.Result;
import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractMapValues;
import platform.server.caches.IdentityLazy;
import platform.server.caches.InnerContext;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.MapValuesTranslator;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.*;
import platform.server.session.*;
import platform.server.Settings;

import java.util.*;

@Aspect
public class MapCacheAspect {
    private final static Logger logger = Logger.getLogger(MapCacheAspect.class);
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

        Result<MapTranslate> translator = new Result<MapTranslate>();
        Query<K,V> mapCache;
        if((mapCache = (Query<K,V>) cache.mapInner(query, true, translator))!=null) {
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
            int hashQuery = query.hashInner(true);
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
            Query cache;
            if(BaseUtils.onlyObjects(query.mapKeys.keySet()) && BaseUtils.onlyObjects(query.properties.keySet())) {
                parsed = (ParsedQuery<K, V>) thisJoinPoint.proceed();
                cache = query;
            } else { // чтобы не было утечки памяти, "заменяем" компилируемый запрос на объекты
                Map<K,Object> genKeys = BaseUtils.generateObjects(query.mapKeys.keySet());
                Map<V,Object> genProps = BaseUtils.generateObjects(query.properties.keySet());
                
                cache = new Query<Object, Object>(BaseUtils.crossJoin(genKeys, query.mapKeys), BaseUtils.crossJoin(genProps, query.properties), query.where);
                ParsedQuery<Object,Object> parsedCache = (ParsedQuery<Object,Object>) thisJoinPoint.proceed(new Object[]{cache, cache});
                ((ParseInterface) cache).setParse(parsedCache);

                parsed = new MapParsedQuery<K, V, Object, Object>(parsedCache, genProps, genKeys, MapValuesTranslator.noTranslate);
            }

            ((ParseInterface) query).setParse(parsed);
            hashCaches.add(cache);
            return parsed;
        }
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

    class JoinImplement<K> extends InnerContext<JoinImplement<K>> {
        final Map<K,? extends Expr> exprs;
        final MapValuesTranslate mapValues; // map context'а values на те которые нужны

        JoinImplement(Map<K, ? extends Expr> exprs,MapValuesTranslate mapValues) {
            this.exprs = exprs;
            this.mapValues = mapValues;
        }

        @IdentityLazy
        public Set<KeyExpr> getKeys() {
            return AbstractSourceJoin.enumKeys(exprs.values());
        }

        @IdentityLazy
        public Set<Value> getValues() {
            // нельзя из values так как вообще не его контекст
            return BaseUtils.mergeSet(AbstractSourceJoin.enumValues(exprs.values()), mapValues.getValues());
        }

        @IdentityLazy
        public int hashInner(HashContext hashContext) {
            int hash=0;
            for(Map.Entry<K,? extends Expr> expr : exprs.entrySet())
                hash += expr.getKey().hashCode() ^ expr.getValue().hashOuter(hashContext);
            return hash * 31 + mapValues.hash(hashContext.values);
        }

        public JoinImplement<K> translateInner(MapTranslate translate) {
            return new JoinImplement<K>(translate.translate(exprs), mapValues.map(translate.mapValues()));
        }

        public boolean equalsInner(JoinImplement<K> object) {
            return BaseUtils.hashEquals(exprs,object.exprs) && BaseUtils.hashEquals(mapValues,object.mapValues);
        }
    }

    <K,V> Join<V> join(Map<K,? extends Expr> joinExprs, MapValuesTranslate joinValues,Map<Integer,Map<JoinImplement<K>,Join<V>>> joinCaches,ProceedingJoinPoint thisJoinPoint) throws Throwable {
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
            Join<V> join = (Join<V>) thisJoinPoint.proceed();
            hashCaches.put(joinImplement,join);
            return join;
        }
    }

    @Around("call(platform.server.data.query.Join platform.server.data.query.ParsedJoinQuery.joinExprs(java.util.Map,platform.server.data.translator.MapValuesTranslate)) && target(query) && args(joinExprs,mapValues)")
    public Object callJoin(ProceedingJoinPoint thisJoinPoint, ParsedJoinQuery query, Map joinExprs, MapValuesTranslate mapValues) throws Throwable {
        return join(joinExprs,mapValues,((JoinInterface)query).getJoinCache(),thisJoinPoint);
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

        if(!(property instanceof FunctionProperty) && !(property instanceof UserProperty && ((UserProperty)property).derivedChange!=null)) // если не Function или DataProperty с derived, то нету рекурсии и эффективнее просто вы
            return (U) thisJoinPoint.proceed(); 

        U implement = modifier.fullChanges();

        if(implement.getValues().size() > Settings.instance.getUsedChangesCacheLimit())
            return (U) thisJoinPoint.proceed();

        Map<U, U> hashCaches;
        synchronized(usedCaches) {
            int hashImplement = implement.getComponents().hash;
            hashCaches = usedCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new HashMap<U, U>();
                usedCaches.put(hashImplement, hashCaches);
            }
        }

        synchronized(hashCaches) {
            for(Map.Entry<U,U> cache : hashCaches.entrySet()) {
                for(MapValuesTranslate mapValues : new MapValuesIterable(cache.getKey(), implement)) {
                    if(cache.getKey().translate(mapValues).equals(implement)) {
                        logger.info("getUsedChanges - cached "+property);
                        U cacheResult = cache.getValue().translate(mapValues);
                        if(!modifier.neededClass(cacheResult)) {
                            assert !cacheResult.modifyUsed();
                            return modifier.newChanges().addChanges(cacheResult);
                        }
                        return cacheResult;
                    }
                }
            }

            logger.info("getUsedChanges - not cached "+property);
            U usedChanges = (U) thisJoinPoint.proceed();
            hashCaches.put(implement, usedChanges);

            return usedChanges;
        }
    }

    @Around("call(* platform.server.logics.property.Property.aspectGetUsedChanges(platform.server.session.Modifier)) " +
            "&& target(property) && args(modifier) " +
            "&& !cflowbelow(call(* platform.server.logics.property.Property.aspectGetUsedChanges(platform.server.session.Modifier)) || call(* platform.server.logics.property.Property.calculateExpr(java.util.Map,platform.server.session.Modifier,platform.server.data.where.WhereBuilder)))")
    public Object callUpGetUsedChanges(ProceedingJoinPoint thisJoinPoint, Property property, Modifier modifier) throws Throwable {
        return getUsedChanges(property, modifier, ((MapPropertyInterface)property).getUsedChangesCache(), thisJoinPoint);
    }

    @Around("call(* platform.server.logics.property.Property.aspectGetUsedChanges(*)) " +
            "&& target(property) && args(modifier) " +
            "&& cflowbelow(call(* platform.server.logics.property.Property.aspectGetUsedChanges(platform.server.session.Modifier)) || call(* platform.server.logics.property.Property.calculateExpr(java.util.Map,platform.server.session.Modifier,platform.server.data.where.WhereBuilder)))")
    public Object callRecGetUsedChanges(ProceedingJoinPoint thisJoinPoint, Property property, Modifier modifier) throws Throwable {
        if(modifier instanceof IncrementApply) // сначала target в аспекте должен быть
            return thisJoinPoint.proceed();
        else
            return getUsedChanges(property, modifier, ((MapPropertyInterface)property).getUsedChangesCache(), thisJoinPoint);
    }

    final String PROPERTY_STRING = "expr";
    final String CHANGED_STRING = "where";

    static class JoinExprInterfaceImplement<U extends Changes<U>> extends AbstractMapValues<JoinExprInterfaceImplement<U>> {
        final U usedChanges;
        final boolean changed; // нужно ли условие на изменение, по сути для этого св-ва и делается класс

        JoinExprInterfaceImplement(Property<?> property, Modifier<U> modifier, boolean changed) {
            usedChanges = property.getUsedChanges(modifier);
            this.changed = changed;
        }

        public boolean twins(TwinImmutableInterface o) {
            return changed == ((JoinExprInterfaceImplement) o).changed && usedChanges.equals(((JoinExprInterfaceImplement) o).usedChanges);
        }

        @IdentityLazy
        public int hashValues(HashValues hashValues) {
            return 31 * usedChanges.hashValues(hashValues) + (changed ? 1 : 0);
        }

        public Set<Value> getValues() {
            return usedChanges.getValues();
        }

        JoinExprInterfaceImplement(JoinExprInterfaceImplement<U> implement, MapValuesTranslate mapValues) {
            usedChanges = implement.usedChanges.translate(mapValues);
            this.changed = implement.changed;
        }

        public JoinExprInterfaceImplement<U> translate(MapValuesTranslate mapValues) {
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
            int hashImplement = implement.getComponents().hash;
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
                for(MapValuesTranslate mapValues : new MapValuesIterable(cache.getKey(), implement)) {
                    if(cache.getKey().translate(mapValues).equals(implement)) {
                        cached = true;
                        queryJoin = cache.getValue().join(joinExprs,mapValues.filter(cache.getValue().getValues())); // так как могут не использоваться values в Query
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

                logger.info("getExpr - not cached "+property);
                queryJoin = query.join(joinExprs);
            }
        }

        if(changedWheres!=null) changedWheres.add(queryJoin.getExpr(CHANGED_STRING).getWhere());

        Expr result = queryJoin.getExpr(PROPERTY_STRING);
        if(cached) {
            logger.info("getExpr - cached "+property);
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

    static class DataChangesInterfaceImplement<P extends PropertyInterface, U extends Changes<U>> extends InnerContext<DataChangesInterfaceImplement<P,U>> {
        final U usedChanges;
        final PropertyChange<P> change;
        final boolean where;

        DataChangesInterfaceImplement(Property<P> property, PropertyChange<P> change, Modifier<U> modifier, boolean where) {
            usedChanges = property.getUsedDataChanges(modifier);
            this.change = change;
            this.where = where;
        }

        public boolean equalsInner(DataChangesInterfaceImplement<P, U> o) {
            return BaseUtils.hashEquals(change,o.change) && BaseUtils.hashEquals(usedChanges,o.usedChanges) && where == o.where;
        }

        @IdentityLazy
        public int hashInner(HashContext hashContext) {
            return 31 * usedChanges.hashValues(hashContext.values) + change.hashInner(hashContext) + (where?1:0);
        }

        public Set<KeyExpr> getKeys() {
            return change.getKeys();
        }

        public Set<Value> getValues() {
            Set<Value> result = new HashSet<Value>();
            result.addAll(usedChanges.getValues());
            result.addAll(change.getValues());
            return result;
        }

        DataChangesInterfaceImplement(DataChangesInterfaceImplement<P,U> implement, MapTranslate translator) {
            usedChanges = implement.usedChanges.translate(translator.mapValues());
            change = implement.change.translateInner(translator);
            this.where = implement.where;
        }

        public DataChangesInterfaceImplement<P,U> translateInner(MapTranslate translator) {
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
                    return ((DataChangesResult<K>)cache.getValue()).changes.translate(translator.mapValues());
                }
            }

            WhereBuilder cacheWheres = Property.cascadeWhere(changedWheres);
            MapDataChanges<K> changes = (MapDataChanges<K>) thisJoinPoint.proceed(new Object[]{property,property,change,cacheWheres,modifier});
            if(Settings.instance.packOnCacheComplexity > 0 && changes.getComplexity() > Settings.instance.packOnCacheComplexity)
                changes = changes.pack(); // пакуем так как в кэш складываем
            hashCaches.put(implement, new DataChangesResult<K>(changes, changedWheres!=null?cacheWheres.toWhere():null));
            logger.info("getDataChanges - not cached "+property);

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

    static class ExprInterfaceImplement<P extends PropertyInterface, U extends Changes<U>> extends InnerContext<ExprInterfaceImplement<P, U>> {
        final U usedChanges;
        final Map<P, Expr> joinImplement;
        final boolean where;

        ExprInterfaceImplement(Property<P> property, Map<P,Expr> joinImplement, Modifier<U> modifier, boolean where) {
            usedChanges = property.getUsedChanges(modifier);
            this.joinImplement = joinImplement;
            this.where = where;
        }

        public boolean equalsInner(ExprInterfaceImplement<P, U> o) {
            return BaseUtils.hashEquals(joinImplement,o.joinImplement) && BaseUtils.hashEquals(usedChanges,o.usedChanges) && where == o.where;
        }

        @IdentityLazy
        public int hashInner(HashContext hashContext) {
            int hash = 0;
            for(Map.Entry<P,Expr> joinExpr : joinImplement.entrySet())
                hash += joinExpr.getKey().hashCode() * joinExpr.getValue().hashOuter(hashContext);
            return 31 * usedChanges.hashValues(hashContext.values) + hash + (where?1:0);
        }

        public Set<KeyExpr> getKeys() {
            return AbstractSourceJoin.enumKeys(joinImplement.values());
        }

        public Set<Value> getValues() {
            Set<Value> result = new HashSet<Value>();
            result.addAll(usedChanges.getValues());
            result.addAll(AbstractSourceJoin.enumValues(joinImplement.values()));
            return result;
        }

        ExprInterfaceImplement(ExprInterfaceImplement<P,U> implement, MapTranslate translator) {
            usedChanges = implement.usedChanges.translate(translator.mapValues());
            joinImplement = translator.translate(implement.joinImplement);
            this.where = implement.where;
        }

        public ExprInterfaceImplement<P,U> translateInner(MapTranslate translator) {
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

    public static boolean disableCaches = false;

    public <K extends PropertyInterface,U extends Changes<U>> Expr getExpr(Property<K> property, Map<K, Expr> joinExprs, Modifier<U> modifier, WhereBuilder changedWheres, Map<Integer, Map<ExprInterfaceImplement, ExprResult>> exprCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        // здесь по идее на And не надо проверять
        if(disableCaches || property.equals(AggregateProperty.recalculate)) return (Expr) thisJoinPoint.proceed();

        property.cached = true;

        ExprInterfaceImplement<K,U> implement = new ExprInterfaceImplement<K,U>(property,joinExprs,modifier,changedWheres!=null);

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
            for(Map.Entry<ExprInterfaceImplement,ExprResult> cache : hashCaches.entrySet()) {
                MapTranslate translator;
                if((translator=cache.getKey().mapInner(implement, true))!=null) {
                    logger.info("getExpr - cached "+property);
                    if(changedWheres!=null) changedWheres.add(cache.getValue().where.translateOuter(translator));
                    return cache.getValue().expr.translateOuter(translator);
                }
            }

            logger.info("getExpr - not cached "+property);
            WhereBuilder cacheWheres = Property.cascadeWhere(changedWheres);
            Expr expr = (Expr) thisJoinPoint.proceed(new Object[]{property,property,joinExprs,modifier,cacheWheres});
            if(Settings.instance.packOnCacheComplexity > 0 && expr.getComplexity() > Settings.instance.packOnCacheComplexity)
                expr = expr.pack(); // пакуем так как в кэш идет
            hashCaches.put(implement, new ExprResult(expr, changedWheres!=null?cacheWheres.toWhere():null));

//            if(expr.getComplexity()>300) {
//                System.out.println("COMPLEX" + property);
//            }

            // проверим
            if(checkInfinite && !(property instanceof FormulaProperty))
                expr.checkInfiniteKeys();

            if(changedWheres!=null) changedWheres.add(cacheWheres.toWhere());
            return expr;
        }
    }

    public static boolean checkInfinite = false;

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("call(platform.server.data.expr.Expr platform.server.logics.property.Property.getExpr(java.util.Map,platform.server.session.Modifier,platform.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,modifier,changedWhere)")
    public Object callGetExpr(ProceedingJoinPoint thisJoinPoint, Property property, Map joinExprs, Modifier modifier,WhereBuilder changedWhere) throws Throwable {
        // сначала target в аспекте должен быть
//        return getJoinExpr(property, joinExprs, modifier, changedWhere, ((MapPropertyInterface)property).getJoinExprCache(), thisJoinPoint);
        return getExpr(property, joinExprs, modifier, changedWhere, ((MapPropertyInterface)property).getExprCache(), thisJoinPoint);
    }
}
