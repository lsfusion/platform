package lsfusion.server.data.caches;

import lsfusion.base.BaseUtils;
import lsfusion.base.ReflectionUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.add.MAddCol;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.server.base.caches.CacheStats;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.translate.MapQuery;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.translate.MapValuesTranslator;
import lsfusion.server.data.translate.RemapValuesTranslator;
import lsfusion.server.data.value.Value;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class QueryCacheAspect {
    public interface QueryCacheInterface {
        IQuery getCacheTwin();
        void setCacheTwin(IQuery query);
    }
    public static class QueryCacheInterfaceImplement implements QueryCacheInterface {
        IQuery cacheTwin;
        public IQuery getCacheTwin() {
            return cacheTwin;
        }
        public void setCacheTwin(IQuery query) {
            cacheTwin = query;
        }
    }
    @DeclareParents(value="lsfusion.server.data.query.Query",defaultImpl=QueryCacheInterfaceImplement.class)
    private QueryCacheInterface queryCacheInterface;

    static <K,V,CK,CV> MapQuery<CK,CV,K,V> cacheTwinQuery(Query<K,V> cache, Query<CK,CV> query) {

        Result<MapTranslate> translator = new Result<>();
        Query.MultiParamsContext<?, ?> multiParams = cache.getMultiParamsContext().mapInner(query.getMultiParamsContext(), true, translator);
        if(multiParams!=null) {
            Query<K,V> mapCache = (Query<K, V>)multiParams.getQuery();
            ImRevMap<CV,V> mapProps = MapFact.mapValues(query.properties, mapCache.properties);
            return new MapQuery<>(cache, mapProps, query.mapKeys.crossValuesRev(mapCache.mapKeys), translator.result.mapValues());
        }
        return null;
    }

    private final static LRUSVSMap<Integer, MAddCol<Query>> hashTwins = new LRUSVSMap<>(LRUUtil.G2);

    private <K,V> IQuery<K,V> cacheTwin(Query<K,V> query) throws Throwable {
        QueryCacheInterface queryCacheInterface = (QueryCacheInterface) query;

        IQuery<K, V> result = queryCacheInterface.getCacheTwin();
        if(result!=null)
            return result;

        MAddCol<Query> hashCaches;
        int hashQuery = query.getMultiParamsContext().getInnerComponents(true).hash;
        hashCaches = hashTwins.get(hashQuery);
        if(hashCaches==null) {
            hashCaches = ListFact.mAddCol();
            hashTwins.put(hashQuery, hashCaches);
        }
        synchronized(hashCaches) {
            result = queryCacheInterface.getCacheTwin(); // double check, важно что в hashCaches только "самоtwin'ы", и здесь их уже не должно быть (так как заmapp'иться на себя и будет StackOverflow), при этом может быть race condition между верхней проверкой и этой синхронизацией (если один и тот же запрос 2 раза cacheTwin делает)
            if(result!=null)
                return result;

            for(Query<?,?> cache : hashCaches.it()) {
                assert query != cache;
                MapQuery<K,V,?,?> packed = cacheTwinQuery(cache, query);
                if(packed !=null) {
//                    logger.debug("cached");
                    CacheStats.incrementHit(CacheStats.CacheType.QUERY);

                    assert packed.getMapQuery() == cache && ((QueryCacheInterface)cache).getCacheTwin() == cache;
                    queryCacheInterface.setCacheTwin(packed);
                    return packed;
                }
            }
//            logger.debug("not cached");
            CacheStats.incrementMissed(CacheStats.CacheType.QUERY);
            Result<Query> cache = new Result<>();
            result = cacheNoBigTwin(query, cache);
            ((QueryCacheInterface)cache.result).setCacheTwin(cache.result);
            hashCaches.add(cache.result);

            queryCacheInterface.setCacheTwin(result);
            return result;
        }
    }

    private <K,V> IQuery<K,V> cacheNoBigTwin(Query<K, V> query, Result<Query> cacheTwin) {
        ImSet<Value> contextValues = query.getContextValues();
        ImRevMap<Value, Value> bigValues = AbstractValuesContext.getBigValues(contextValues);
        if(BaseUtils.onlyObjects(query.mapKeys.keyIt()) && BaseUtils.onlyObjects(query.properties.keyIt()) && bigValues == null) {
            cacheTwin.set(query);
            return query;
        } else { // чтобы не было утечки памяти, "заменяем" компилируемый запрос на объекты, а все большие значения на поменьше
            ImRevMap<K,Object> genKeys = BaseUtils.generateObjects(query.mapKeys.keys());
            ImRevMap<V,Object> genProps = BaseUtils.generateObjects(query.properties.keys());

            Query<Object, Object> cache = new Query<>(genKeys.crossJoin(query.mapKeys), genProps.crossJoin(query.properties), query.where);

            if(bigValues!=null) // bigvalues - работа с транслированными объектами, а в конце трансляция назад
                cache = cache.translateQuery(new RemapValuesTranslator(bigValues));

            cacheTwin.set(cache);

            return new MapQuery<>(cache, genProps, genKeys,
                    bigValues == null ? MapValuesTranslator.noTranslate(contextValues) : new RemapValuesTranslator(bigValues.reverse()));
        }
    }

    @Around("execution(@lsfusion.server.base.caches.ContextTwin * *.*(..)) && target(query)")
    public Object callContextTwinMethod(ProceedingJoinPoint thisJoinPoint, Query query) throws Throwable {
        IQuery cache = cacheTwin(query);
        if(cache!=query) {
            MethodSignature signature = (MethodSignature) thisJoinPoint.getSignature();
            return ReflectionUtils.invokeTransp(cache.getClass().getMethod(signature.getName(), signature.getParameterTypes()), cache, thisJoinPoint.getArgs());
        }
        return thisJoinPoint.proceed();
    }
}
