package platform.server.data.query;

import net.jcip.annotations.Immutable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import platform.base.BaseUtils;
import platform.server.caches.Lazy;
import platform.server.caches.MapContext;
import platform.server.caches.MapParamsIterable;
import platform.server.data.classes.LogicalClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.logics.BusinessLogics;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.properties.DataProperty;
import platform.server.session.TableChanges;
import platform.server.where.WhereBuilder;

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
    @DeclareParents(value="platform.server.data.query.JoinQuery",defaultImpl=ParseInterfaceImplement.class)
    private ParseInterface parseInterface;

    static <K,V,CK,CV> MapParsedQuery<CK,CV,K,V> cacheQuery(JoinQuery<K,V> cache,JoinQuery<CK,CV> query) {
        for(KeyTranslator translator : new MapParamsIterable(cache, query, true)) {
            Map<CV,V> mapProps;
            if(cache.where.translate(translator).equals(query.where) && (mapProps=BaseUtils.mapEquals(query.properties,translator.translate(cache.properties)))!=null)
                return new MapParsedQuery<CK,CV,K,V>((ParsedJoinQuery<K,V>) ((ParseInterface)cache).getParse(),
                        mapProps,BaseUtils.crossValues(query.mapKeys, cache.mapKeys, translator.keys),translator.values);
        }
        return null;
    }                        

    final static Map<Integer, Collection<JoinQuery>> cacheParse = new HashMap<Integer, Collection<JoinQuery>>();
    <K,V> ParsedQuery<K,V> parse(JoinQuery<K,V> query,ProceedingJoinPoint thisJoinPoint) throws Throwable {
        ParsedQuery<K,V> parsed = ((ParseInterface) query).getParse();
        if(parsed!=null) return parsed;

        Collection<JoinQuery> hashCaches;
        synchronized(cacheParse) {
            int hashQuery = MapParamsIterable.hash(query,true);
            hashCaches = cacheParse.get(hashQuery);
            if(hashCaches==null) {
                hashCaches = new ArrayList<JoinQuery>();
                cacheParse.put(hashQuery, hashCaches);
            }
        }
//        synchronized(hashCaches) {
            for(JoinQuery<?,?> cache : hashCaches) {
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

    @Around("call(platform.server.data.query.ParsedQuery platform.server.data.query.JoinQuery.parse()) && target(query)")
    public Object callParse(ProceedingJoinPoint thisJoinPoint, JoinQuery query) throws Throwable {
        return parse(query,thisJoinPoint);
    }

    public interface JoinInterface {
        Map getJoinCache();
    }
    public static class JoinInterfaceImplement implements JoinInterface {
        Map<JoinImplement,Join> joinCache = new HashMap<JoinImplement,Join>();
        public Map getJoinCache() { return joinCache; }
    }
    @DeclareParents(value="platform.server.data.query.ParsedJoinQuery",defaultImpl=JoinInterfaceImplement.class)
    private JoinInterface joinInterface;

    @Immutable
    class JoinImplement<K> implements MapContext {
        final Map<K,? extends SourceExpr> exprs;
        final Map<ValueExpr,ValueExpr> values; // map context'а values на те которые нужны

        JoinImplement(Map<K, ? extends SourceExpr> iExprs,Map<ValueExpr,ValueExpr> iValues) {
            exprs = iExprs;
            values = iValues;
        }

        @Lazy
        public Context getContext() {
            Context context;
            context = new Context();
            context.fill(exprs);
            return context;
        }

        public int hash(HashContext hashContext) {
            int hash=0;
            for(Map.Entry<K,? extends SourceExpr> expr : exprs.entrySet())
                hash += expr.getKey().hashCode() ^ expr.getValue().hashContext(hashContext);
            return hash;
        }
    }

    <K,V> Join<V> join(Map<K,? extends SourceExpr> joinExprs,Map<ValueExpr, ValueExpr> joinValues,Map<Integer,Map<JoinImplement<K>,Join<V>>> joinCaches,ProceedingJoinPoint thisJoinPoint) throws Throwable {
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
            for(Map.Entry<JoinImplement<K>,Join<V>> cache : hashCaches.entrySet()) {
                for(KeyTranslator translator : new MapParamsIterable(cache.getKey(), joinImplement, true)) {
                    if(translator.translate(cache.getKey().exprs).equals(joinImplement.exprs)) {
                        // здесь не все values нужно докинуть их из контекста (ключи по идее все)
                        Map<ValueExpr,ValueExpr> transValues;
                        if((transValues=BaseUtils.mergeEqual(translator.values,BaseUtils.crossJoin(cache.getKey().values,joinImplement.values)))!=null) {
                            System.out.println("join cached");
                            return new TranslateJoin<V>(new KeyTranslator(translator.keys,transValues),cache.getValue());
                        }
                    } else
                        cache = cache;
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

    static class SourceExprImplement<U extends Property.TableUsedChanges<U>> {
        final U usedChanges; 
        final boolean changed; // нужно ли условие на изменение, по сути для этого св-ва и делается класс

        SourceExprImplement(Property<?> property,TableChanges sessionChanges,Collection<DataProperty> usedDefault, Property.TableDepends<U> depends, boolean iChanged) {
            if(sessionChanges==null)
                usedChanges = depends.newChanges();
            else
                usedChanges = property.getUsedChanges(sessionChanges, usedDefault, depends);
            changed = iChanged;
        }

        public boolean equals(Object o) {
            return this == o || o instanceof SourceExprImplement && changed == ((SourceExprImplement) o).changed && usedChanges.equals(((SourceExprImplement) o).usedChanges);
        }

        public int hashCode() {
            return 31 * usedChanges.hashCode() + (changed ? 1 : 0);
        }
    }

    public interface SourceExprInterface {
        Map getSourceExprCache();
    }
    public static class SourceExprInterfaceImplement implements SourceExprInterface {
        Map<SourceExprImplement,JoinQuery> exprCache = new HashMap<SourceExprImplement, JoinQuery>();
        public Map getSourceExprCache() { return exprCache; }
    }
    @DeclareParents(value="platform.server.logics.properties.Property",defaultImpl=SourceExprInterfaceImplement.class)
    private SourceExprInterface sourceExprInterface;

    final String PROPERTY_STRING = "expr";
    final String CHANGED_STRING = "where";

    public <K extends PropertyInterface,U extends Property.TableUsedChanges<U>> SourceExpr getSourceExpr(Property<K> property,Map<K,SourceExpr> joinExprs,TableChanges changes, Collection<DataProperty> usedDefault,Property.TableDepends<U> depends,WhereBuilder changedWheres,ProceedingJoinPoint thisJoinPoint) throws Throwable {

        // если идет autoFillDB то не кэшируем
        if(BusinessLogics.autoFillDB) return (SourceExpr) thisJoinPoint.proceed();

        property.cached = true;

        SourceExprImplement<U> implement = new SourceExprImplement<U>(property,changes,usedDefault,depends,changedWheres!=null);

        Map<SourceExprImplement,JoinQuery<K,String>> exprCache = ((SourceExprInterface)property).getSourceExprCache();

        JoinQuery<K,String> query = exprCache.get(implement);
        if(query==null) {
            System.out.println("getSourceExpr - not cached "+property);
            // надо проверить что с такими changes, defaultProps, noUpdateProps
            query = new JoinQuery<K,String>(property);
            WhereBuilder queryWheres = (changedWheres==null?null:new WhereBuilder());
            query.properties.put(PROPERTY_STRING, (SourceExpr) thisJoinPoint.proceed(new Object[]{property,property,query.mapKeys,changes,usedDefault,depends,queryWheres}));
            if(changedWheres!=null)
                query.properties.put(CHANGED_STRING,new ValueExpr(true, LogicalClass.instance).and(queryWheres.toWhere()));

            exprCache.put(implement,query);
        } else
            System.out.println("getSourceExpr - cached "+property);

        Join<String> queryJoin = query.join(joinExprs);
        if(changedWheres!=null) changedWheres.add(queryJoin.getExpr(CHANGED_STRING).getWhere());
        return queryJoin.getExpr(PROPERTY_STRING);
    }

    // aspect который ловит getSourceExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("call(platform.server.data.query.exprs.SourceExpr platform.server.logics.properties.Property.getSourceExpr(java.util.Map,platform.server.session.TableChanges,java.util.Collection,platform.server.logics.properties.Property.TableDepends,platform.server.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,changes,usedDefault,depends,changedWhere)")
    public Object callGetSourceExpr(ProceedingJoinPoint thisJoinPoint, Property property, Map joinExprs, TableChanges changes, Collection usedDefault, Property.TableDepends depends,WhereBuilder changedWhere) throws Throwable {
        // сначала target в аспекте должен быть
        return getSourceExpr(property, joinExprs, changes, usedDefault, depends, changedWhere, thisJoinPoint);
    }
}
