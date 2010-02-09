package platform.server.data.query;

import net.jcip.annotations.Immutable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import platform.base.BaseUtils;
import platform.server.caches.MapContext;
import platform.server.caches.MapHashIterable;
import platform.server.caches.MapParamsIterable;
import platform.server.caches.Lazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.KeyTranslator;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.AndFormulaProperty;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.data.where.WhereBuilder;

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
        for(KeyTranslator translator : new MapHashIterable(cache, query, true)) {
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
        Map<JoinImplement,Join> joinCache = new HashMap<JoinImplement,Join>();
        public Map getJoinCache() { return joinCache; }
    }
    @DeclareParents(value="platform.server.data.query.ParsedJoinQuery",defaultImpl=JoinInterfaceImplement.class)
    private JoinInterface joinInterface;

    @Immutable
    class JoinImplement<K> implements MapContext {
        final Map<K,? extends Expr> exprs;
        final Map<ValueExpr,ValueExpr> mapValues; // map context'а values на те которые нужны

        JoinImplement(Map<K, ? extends Expr> iExprs,Map<ValueExpr,ValueExpr> iValues) {
            exprs = iExprs;
            mapValues = iValues;
        }

        @Lazy
        public Set<KeyExpr> getKeys() {
            return AbstractSourceJoin.enumKeys(exprs.values());
        }

        @Lazy
        public Set<ValueExpr> getValues() {
            // нельзя из values так как вообще не его контекст
            return AbstractSourceJoin.enumValues(exprs.values());
        }

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
            for(Map.Entry<JoinImplement<K>,Join<V>> cache : hashCaches.entrySet()) {
                for(KeyTranslator translator : new MapHashIterable(cache.getKey(), joinImplement, true)) {
                    if(translator.translate(cache.getKey().exprs).equals(joinImplement.exprs)) {
                        // здесь не все values нужно докинуть их из контекста (ключи по идее все)
                        Map<ValueExpr,ValueExpr> transValues;
                        if((transValues=BaseUtils.mergeEqual(translator.values,BaseUtils.crossJoin(cache.getKey().mapValues,joinImplement.mapValues)))!=null) {
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

    static class InterfaceImplement<U extends TableChanges<U>> {
        final U usedChanges; 
        final boolean changed; // нужно ли условие на изменение, по сути для этого св-ва и делается класс

        InterfaceImplement(Property<?> property, TableModifier<U> modifier, boolean changed) {
            usedChanges = property.getUsedChanges(modifier);
            this.changed = changed;
        }

        public boolean equals(Object o) {
            return this == o || o instanceof InterfaceImplement && changed == ((InterfaceImplement) o).changed && usedChanges.equals(((InterfaceImplement) o).usedChanges);
        }

        public int hashCode() {
            return 31 * usedChanges.hashCode() + (changed ? 1 : 0);
        }
    }

    public interface ExprInterface {
        Map getExprCache();
    }
    public static class ExprInterfaceImplement implements ExprInterface {
        Map<Integer,Collection<InterfaceImplement>> exprCache = new HashMap<Integer, Collection<InterfaceImplement>>();
        public Map getExprCache() { return exprCache; }
    }
    @DeclareParents(value="platform.server.logics.property.Property",defaultImpl= ExprInterfaceImplement.class)
    private ExprInterface exprInterface;

    final String PROPERTY_STRING = "expr";
    final String CHANGED_STRING = "where";

    public <K extends PropertyInterface,U extends TableChanges<U>> Expr getExpr(Property<K> property, Map<K, Expr> joinExprs, TableModifier<U> modifier, WhereBuilder changedWheres, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        // если свойство AndFormulaProperty - то есть нарушается инвариант что все входные не null идет autoFillDB то не кэшируем
        if(property instanceof AndFormulaProperty || BusinessLogics.autoFillDB) return (Expr) thisJoinPoint.proceed();

        property.cached = true;

        InterfaceImplement<U> implement = new InterfaceImplement<U>(property,modifier,changedWheres!=null);

        Map<InterfaceImplement, Query<K,String>> exprCache = ((ExprInterface)property).getExprCache();

        Query<K,String> query = exprCache.get(implement);
        if(query==null) {
            System.out.println("getExpr - not cached "+property);
            // надо проверить что с такими changes, defaultProps, noUpdateProps
            query = new Query<K,String>(property);
            WhereBuilder queryWheres = (changedWheres==null?null:new WhereBuilder());
            query.properties.put(PROPERTY_STRING, (Expr) thisJoinPoint.proceed(new Object[]{property,property,query.mapKeys,modifier,queryWheres}));
            if(changedWheres!=null)
                query.properties.put(CHANGED_STRING,ValueExpr.get(queryWheres.toWhere()));

            exprCache.put(implement,query);
        } else
            System.out.println("getExpr - cached "+property);

        Join<String> queryJoin = query.join(joinExprs);

        if(changedWheres!=null) changedWheres.add(queryJoin.getExpr(CHANGED_STRING).getWhere());
        return queryJoin.getExpr(PROPERTY_STRING);
    }

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("call(platform.server.data.expr.Expr platform.server.logics.property.Property.getExpr(java.util.Map,platform.server.session.TableModifier,platform.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,modifier,changedWhere)")
    public Object callGetExpr(ProceedingJoinPoint thisJoinPoint, Property property, Map joinExprs, TableModifier modifier,WhereBuilder changedWhere) throws Throwable {
        // сначала target в аспекте должен быть
        return getExpr(property, joinExprs, modifier, changedWhere, thisJoinPoint);
    }
}
