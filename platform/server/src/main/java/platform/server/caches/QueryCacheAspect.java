package platform.server.caches;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import org.aspectj.lang.reflect.MethodSignature;
import platform.base.BaseUtils;
import platform.base.Result;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.add.MAddCol;
import platform.base.col.lru.LRUCache;
import platform.base.col.lru.MCacheMap;
import platform.server.data.Value;
import platform.server.data.query.IQuery;
import platform.server.data.query.MapQuery;
import platform.server.data.query.Query;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslator;

@Aspect
public class QueryCacheAspect {
    private final static Logger logger = Logger.getLogger(QueryCacheAspect.class);

    public interface QueryCacheInterface {
        MCacheMap getJoinCache();
        
        IQuery getCacheTwin();
        void setCacheTwin(IQuery query);
    }
    public static class QueryCacheInterfaceImplement implements QueryCacheInterface {
        MCacheMap joinCache = LRUCache.mSmall(LRUCache.EXP_RARE);

        public MCacheMap getJoinCache() { return joinCache; }

        IQuery cacheTwin;
        public IQuery getCacheTwin() {
            return cacheTwin;
        }
        public void setCacheTwin(IQuery query) {
            cacheTwin = query;
        }
    }
    @DeclareParents(value="platform.server.data.query.Query",defaultImpl=QueryCacheInterfaceImplement.class)
    private QueryCacheInterface queryCacheInterface;

    static <K,V,CK,CV> MapQuery<CK,CV,K,V> cacheTwinQuery(Query<K,V> cache, Query<CK,CV> query) {

        Result<MapTranslate> translator = new Result<MapTranslate>();
        Query.MultiParamsContext<?, ?> multiParams = cache.getMultiParamsContext().mapInner(query.getMultiParamsContext(), true, translator);
        if(multiParams!=null) {
            Query<K,V> mapCache = (Query<K, V>)multiParams.getQuery();
            ImRevMap<CV,V> mapProps = MapFact.mapValues(query.properties, mapCache.properties);
            return new MapQuery<CK,CV,K,V>(cache, mapProps, query.mapKeys.crossValuesRev(mapCache.mapKeys),translator.result.mapValues());
        }
        return null;
    }

    final static MCacheMap<Integer, MAddCol<Query>> hashTwins = LRUCache.mBig();

    <K,V> IQuery<K,V> cacheTwin(Query<K,V> query) throws Throwable {
        IQuery<K, V> result = ((QueryCacheInterface)query).getCacheTwin();
        if(result!=null)
            return result;

        MAddCol<Query> hashCaches;
        synchronized(hashTwins) {
            int hashQuery = query.getMultiParamsContext().getInnerComponents(true).hash;
            hashCaches = hashTwins.get(hashQuery);
            if(hashCaches==null) {
                hashCaches = ListFact.mAddCol();
                hashTwins.exclAdd(hashQuery, hashCaches);
            }
        }
        synchronized(hashCaches) {
            for(Query<?,?> cache : hashCaches.it()) {
                IQuery<K,V> packed = cacheTwinQuery(cache, query);
                if(packed !=null) {
                    logger.debug("cached");

                    ((QueryCacheInterface)query).setCacheTwin(result);
                    return packed;
                }
            }
            logger.debug("not cached");
            Result<Query> cache = new Result<Query>();
            result = cacheNoBigTwin(query, cache);
            ((QueryCacheInterface)cache.result).setCacheTwin(cache.result);
            hashCaches.add(cache.result);

            ((QueryCacheInterface)query).setCacheTwin(result);
            return result;
        }
    }

    private <K,V> IQuery<K,V> cacheNoBigTwin(Query<K, V> query, Result<Query> cacheTwin) throws Throwable {
        ImRevMap<Value, Value> bigValues = AbstractValuesContext.getBigValues(query.getContextValues());
        if(BaseUtils.onlyObjects(query.mapKeys.keyIt()) && BaseUtils.onlyObjects(query.properties.keyIt()) && bigValues == null) {
            cacheTwin.set(query);
            return query;
        } else { // чтобы не было утечки памяти, "заменяем" компилируемый запрос на объекты, а все большие значения на поменьше
            ImRevMap<K,Object> genKeys = BaseUtils.generateObjects(query.mapKeys.keys());
            ImRevMap<V,Object> genProps = BaseUtils.generateObjects(query.properties.keys());

            Query<Object, Object> cache = new Query<Object, Object>(genKeys.crossJoin(query.mapKeys), genProps.crossJoin(query.properties), query.where);

            if(bigValues!=null) // bigvalues - работа с транслированными объектами, а в конце трансляция назад
                cache = cache.translateQuery(new MapValuesTranslator(bigValues));

            cacheTwin.set(cache);

            return new MapQuery<K, V, Object, Object>(cache, genProps, genKeys,
                    bigValues == null ? MapValuesTranslator.noTranslate : new MapValuesTranslator(bigValues.reverse()));
        }
    }

    @Around("execution(@platform.server.caches.ContextTwin * *.*(..)) && target(query)")
    public Object callContextTwinMethod(ProceedingJoinPoint thisJoinPoint, Query query) throws Throwable {
        IQuery cache = cacheTwin(query);
        if(cache!=query) {
            MethodSignature signature = (MethodSignature) thisJoinPoint.getSignature();
            return cache.getClass().getMethod(signature.getName(), signature.getParameterTypes()).invoke(cache, thisJoinPoint.getArgs());
        }
        return thisJoinPoint.proceed();
    }

/*    @Around("execution(* platform.server.data.query.Query.calculatePack()) && target(query)")
    public Object callPack(ProceedingJoinPoint thisJoinPoint, Query query) throws Throwable {
        if(query.cacheTwin!=query) {
            if(query.cacheTwin==null)
                query.cacheTwin = cacheTwin(query);
            return query.cacheTwin.pack();
        }
        return thisJoinPoint.proceed();
    }*/


/*    static <K,V,CK,CV> MapQuery<CK,CV,K,V> cacheQuery(Query<K,V> cache, Query<CK,CV> query) {

        Result<MapTranslate> translator = new Result<MapTranslate>();
        Query.MultiParamsContext<?, ?> multiParams = cache.getMultiParamsContext().mapInner(query.getMultiParamsContext(), true, translator);
        if(multiParams!=null) {
            Query<K,V> mapCache = (Query<K, V>)multiParams.getQuery();
            Map<CV,V> mapProps = BaseUtils.mapColValues(query.properties,mapCache.properties);
            assert cache.packed!=null;
            return new MapQuery<CK,CV,K,V>(cache.pack(),mapProps,BaseUtils.crossValues(query.mapKeys, mapCache.mapKeys),translator.result.mapColValues());
        }
        return null;
    }

    final static Map<Integer, Collection<Query>> cachePack = new HashMap<Integer, Collection<Query>>();
    <K,V> IQuery<K,V, ?> pack(Query<K,V> query,ProceedingJoinPoint thisJoinPoint) throws Throwable {
        Collection<Query> hashCaches;
        synchronized(cachePack) {
            int hashQuery = query.getMultiParamsContext().hashInner(true);
            hashCaches = cachePack.get(hashQuery);
            if(hashCaches==null) {
                hashCaches = new ArrayList<Query>();
                cachePack.put(hashQuery, hashCaches);
            }
        }
        synchronized(hashCaches) {
            for(Query<?,?> cache : hashCaches) {
                IQuery<K,V, ?> packed = cacheQuery(cache, query);
                if(packed !=null) {
                    logger.info("cached");
                    return packed;
                }
            }
            logger.info("not cached");
            Result<Query> cache = new Result<Query>();
            IQuery<K,V, ?> packed = calculatePack(query, thisJoinPoint, cache);
            hashCaches.add(cache.result);
            return packed;
        }
    }

    private <K,V> IQuery<K,V, ?> calculatePack(Query<K, V> query, ProceedingJoinPoint thisJoinPoint, Result<Query> cacheResult) throws Throwable {
        Map<Value, Value> bigValues = query.getBigValues();
        if(BaseUtils.onlyObjects(query.mapKeys.keySet()) && BaseUtils.onlyObjects(query.properties.keySet()) && bigValues == null) {
            cacheResult.set(query);
            return (IQuery<K, V, ?>) thisJoinPoint.proceed();
        } else { // чтобы не было утечки памяти, "заменяем" компилируемый запрос на объекты, а все большие значения на поменьше
            Map<K,Object> genKeys = BaseUtils.generateObjects(query.mapKeys.keySet());
            Map<V,Object> genProps = BaseUtils.generateObjects(query.properties.keySet());

            Query<Object, Object> cache = new Query<Object, Object>(BaseUtils.crossJoin(genKeys, query.mapKeys), BaseUtils.crossJoin(genProps, query.properties),
                    query.where);

            if(bigValues!=null) // bigvalues - работа с транслированными объектами, а в конце трансляция назад
                cache = cache.translateValues(new MapValuesTranslator(bigValues));

            Query<Object,Object> packedCache = (Query<Object,Object>) thisJoinPoint.proceed(new Object[]{cache}); // по сути идем сразу в calculate
            cache.packed = packedCache; // вот тут в явную чтобы еще раз MapCacheAspect не вызывать

            cacheResult.set(cache);
            return new MapQuery<K, V, Object, Object>(packedCache, genProps, genKeys,
                    bigValues==null ? MapValuesTranslator.noTranslate : new MapValuesTranslator(BaseUtils.reverse(bigValues)));
        }
    }

    @Around("execution(platform.server.data.query.IQuery platform.server.data.query.Query.calculatePack()) && target(query)")
    public Object callPack(ProceedingJoinPoint thisJoinPoint, Query query) throws Throwable {
        return pack(query,thisJoinPoint);
    }
     */
}
