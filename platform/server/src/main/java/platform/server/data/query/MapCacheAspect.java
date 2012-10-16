package platform.server.data.query;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import platform.base.*;
import platform.server.caches.*;
import platform.server.caches.hash.HashCodeKeys;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.*;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.*;
import platform.server.session.*;

import java.util.*;

@Aspect
public class MapCacheAspect {
    private final static Logger logger = Logger.getLogger(MapCacheAspect.class);

    public static class CacheResult<I extends ValuesContext, R extends TranslateValues> {
        final I implement;
        final R result;

        private CacheResult(I implement, R result) {
            this.implement = implement;
            this.result = result;
        }
    }

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

    private <K,V> Join<V> join(Query<K,V> query, Map<K, ? extends Expr> joinExprs, MapValuesTranslate joinValues, Map<Integer, Collection<CacheResult<JoinImplement<K>, Join<V>>>> joinCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {
//        assert BaseUtils.onlyObjects(joinExprs.keySet()); он вообщем то не нужен, так как hashCaches хранится для Query, а он уже хранит K
        assert ((QueryCacheAspect.QueryCacheInterface)query).getCacheTwin() == query;

        JoinImplement<K> joinImplement = new JoinImplement<K>(joinExprs,joinValues);

        Collection<CacheResult<JoinImplement<K>, Join<V>>> hashCaches;
        synchronized(joinCaches) {
            int hashImplement = joinImplement.getInnerComponents(true).hash;
            hashCaches = joinCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new ArrayList<CacheResult<JoinImplement<K>, Join<V>>>();
                joinCaches.put(hashImplement, hashCaches);
            }
        }
        synchronized(hashCaches) {
            for(CacheResult<JoinImplement<K>, Join<V>> cache : hashCaches) {
                MapTranslate translator;
                if((translator = cache.implement.mapInner(joinImplement, true))!=null) {
                    // здесь не все values нужно докинуть их из контекста (ключи по идее все)
                    logger.debug("join cached");
                    return new MapJoin<V>(translator,cache.result);
                }
            }
/*            logger.info("join not cached"); //можно было бы так, уже есть translateRemoveValues, но так по аналогии с Query будут сразу кэшироваться нужные вызовы
            Join<V> join = (Join<V>) thisJoinPoint.proceed();
            cacheNoBig(joinImplement, hashCaches, join);
            return join;*/

            // такой вариант предпочтительнее с точки зрения кэшей, но в большинстве случаев сделан без него
            Map<Value, Value> bigValues = AbstractValuesContext.getBigValues(joinImplement.getContextValues());
            if(bigValues == null) {
                Join<V> join = (Join<V>) thisJoinPoint.proceed();
                hashCaches.add(new CacheResult<JoinImplement<K>, Join<V>>(joinImplement, join));
                return join;
            } else { // для предотвращения утечки памяти, bigvalues - работа с транслированными объектами, а в конце трансляция назад
                JoinImplement<K> cacheImplement = joinImplement.translateInner(new MapValuesTranslator(bigValues));

                Join<V> join = (Join<V>) thisJoinPoint.proceed(new Object[]{query, cacheImplement.exprs, cacheImplement.mapValues});
                hashCaches.add(new CacheResult<JoinImplement<K>, Join<V>>(cacheImplement, join));

                return new MapJoin<V>((MapTranslate)new MapValuesTranslator(BaseUtils.reverse(bigValues)), join);
            }
        }
    }

    @Around("execution(platform.server.data.query.Join platform.server.data.query.Query.joinExprs(java.util.Map,platform.server.data.translator.MapValuesTranslate)) && target(query) && args(joinExprs,mapValues)")
    public Object callJoin(ProceedingJoinPoint thisJoinPoint, Query query, Map joinExprs, MapValuesTranslate mapValues) throws Throwable {
        return join(query, joinExprs, mapValues, ((QueryCacheAspect.QueryCacheInterface) query).getJoinCache(), thisJoinPoint);
    }

    public interface MapPropertyInterface {
        Map getExprCache();
        Map getJoinExprCache();
        Map getDataChangesCache();
    }
    public static class MapPropertyInterfaceImplement implements MapPropertyInterface {
        Map<Integer,Collection<CacheResult<JoinExprInterfaceImplement,Query>>> exprCache = new HashMap<Integer, Collection<CacheResult<JoinExprInterfaceImplement, Query>>>();
        public Map getExprCache() { return exprCache; }

        Map<Integer,Collection<CacheResult<QueryInterfaceImplement,Query>>> joinExprCache = new HashMap<Integer, Collection<CacheResult<QueryInterfaceImplement, Query>>>();
        public Map getJoinExprCache() { return joinExprCache; }

        Map<Integer,Collection<CacheResult<DataChangesInterfaceImplement,DataChangesResult>>> dataChangesCache = new HashMap<Integer, Collection<CacheResult<DataChangesInterfaceImplement, DataChangesResult>>>();
        public Map getDataChangesCache() { return dataChangesCache; }
    }
    @DeclareParents(value="platform.server.logics.property.CalcProperty",defaultImpl= MapPropertyInterfaceImplement.class)
    private MapPropertyInterface mapPropertyInterface;

    private static boolean checkCaches = false;

    public <K extends PropertyInterface> QuickSet<CalcProperty> getUsedChanges(CalcProperty<K> property, StructChanges implement, boolean cascade, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        if(!(property instanceof FunctionProperty) && !(property instanceof DataProperty && ((DataProperty) property).event!=null)) // если не Function или DataProperty с derived, то нету рекурсии и эффективнее просто вы
            return (QuickSet<CalcProperty>) thisJoinPoint.proceed();

        return (QuickSet<CalcProperty>) CacheAspect.callMethod(property, thisJoinPoint);
    }

    @Around("execution(* platform.server.logics.property.CalcProperty.getUsedChanges(platform.server.session.StructChanges,boolean)) " +
            "&& target(property) && args(changes,cascade)")
    public Object callGetUsedChanges(ProceedingJoinPoint thisJoinPoint, CalcProperty property, StructChanges changes, boolean cascade) throws Throwable {
        return getUsedChanges(property, changes, cascade, thisJoinPoint);
    }

    // все равно надо делать класс в котором будет :
    // propertyChange и getUsedDataChanges

    static class DataChangesInterfaceImplement<P extends PropertyInterface> extends AbstractInnerContext<DataChangesInterfaceImplement<P>> {
        final PropertyChanges usedChanges;
        final PropertyChange<P> change;
        final boolean where;

        DataChangesInterfaceImplement(CalcProperty<P> property, PropertyChange<P> change, PropertyChanges changes, boolean where) {
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

    static class DataChangesResult<P extends PropertyInterface> extends AbstractTranslateValues<DataChangesResult<P>> {
        DataChanges changes;
        Where where;

        DataChangesResult(DataChanges changes, Where where) {
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

    public <K extends PropertyInterface> DataChanges getDataChanges(CalcProperty<K> property, PropertyChange<K> change, WhereBuilder changedWheres, PropertyChanges propChanges, Map<Integer,Collection<CacheResult<DataChangesInterfaceImplement,DataChangesResult>>> dataChangesCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        DataChangesInterfaceImplement<K> implement = new DataChangesInterfaceImplement<K>(property,change,propChanges,changedWheres!=null);

        Collection<CacheResult<DataChangesInterfaceImplement, DataChangesResult>> hashCaches;
        synchronized(dataChangesCaches) {
            int hashImplement = implement.getInnerComponents(true).hash;
            hashCaches = dataChangesCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new ArrayList<CacheResult<DataChangesInterfaceImplement, DataChangesResult>>();
                dataChangesCaches.put(hashImplement, hashCaches);
            }
        }

        synchronized(hashCaches) {
            for(CacheResult<DataChangesInterfaceImplement, DataChangesResult> cache : hashCaches) {
                MapTranslate translator;
                if((translator=cache.implement.mapInner(implement, true))!=null) {
                    logger.debug("getDataChanges - cached "+property);
                    if(changedWheres!=null) changedWheres.add(cache.result.where.translateOuter(translator));
                    return ((DataChangesResult<K>)cache.result).changes.translateValues(translator.mapValues());
                }
            }

            logger.debug("getDataChanges - not cached "+property);
            WhereBuilder cacheWheres = CalcProperty.cascadeWhere(changedWheres);
            DataChanges changes = (DataChanges) thisJoinPoint.proceed(new Object[]{property, change, propChanges, cacheWheres});

            cacheNoBig(implement, hashCaches, new DataChangesResult<K>(changes, changedWheres != null ? cacheWheres.toWhere() : null));

            if(changedWheres!=null) changedWheres.add(cacheWheres.toWhere());
            return changes;
        }
    }

    private <I extends ValuesContext<I>, R extends TranslateValues<R>> void cacheNoBig(I implement, Collection<CacheResult<I, R>> hashCaches, R result) {
        Map<Value, Value> bigValues = AbstractValuesContext.getBigValues(implement.getContextValues());
        if(bigValues == null) // если нет больших значений просто записываем
            hashCaches.add(new CacheResult<I, R>(implement, result));
        else { // bigvalues - работа со старыми объектами, а сохранение транслированных
            MapValuesTranslator removeBig = new MapValuesTranslator(bigValues);
            hashCaches.add(new CacheResult<I, R>(implement.translateRemoveValues(removeBig), result.translateRemoveValues(removeBig)));
        }
    }

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("execution(platform.server.session.DataChanges platform.server.logics.property.CalcProperty.getDataChanges(platform.server.session.PropertyChange,platform.server.session.PropertyChanges,platform.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(change,propChanges,changedWhere)")
    public Object callGetDataChanges(ProceedingJoinPoint thisJoinPoint, CalcProperty property, PropertyChange change, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        // сначала target в аспекте должен быть
        return getDataChanges(property, change, changedWhere, propChanges, ((MapPropertyInterface)property).getDataChangesCache(), thisJoinPoint);
    }

    public static class QueryInterfaceImplement<K extends PropertyInterface> extends AbstractValuesContext<QueryInterfaceImplement<K>> {
        private final PropertyChanges usedChanges;
        private final PropertyQueryType changed; // нужно ли условие на изменение, по сути для этого св-ва и делается класс
        private final Map<K, ? extends Expr> values;
        private final boolean propClasses;

        QueryInterfaceImplement(CalcProperty<?> property, boolean propClasses, PropertyChanges changes, PropertyQueryType changed, Map<K, ? extends Expr> values) {
            usedChanges = property.getUsedChanges(changes);
            this.propClasses = propClasses;
            this.changed = (changed == PropertyQueryType.RECURSIVE ? PropertyQueryType.CHANGED : changed);
            this.values = values;
        }

        protected QuickSet<KeyExpr> getKeys() {
            return QuickSet.EMPTY();
        }

        @Override
        public boolean twins(TwinImmutableInterface o) {
            return changed == ((QueryInterfaceImplement<K>)o).changed && propClasses == ((QueryInterfaceImplement<K>)o).propClasses && usedChanges.equals(((QueryInterfaceImplement<K>)o).usedChanges) && values.equals(((QueryInterfaceImplement<K>)o).values);
        }

        protected boolean isComplex() {
            return true;
        }
        protected int hash(HashValues hashValues) {
            int hash = 0;
            for(Map.Entry<K, ? extends Expr> value : values.entrySet())
                hash += value.getKey().hashCode() ^ value.getValue().hashOuter(new HashContext(HashCodeKeys.instance, hashValues));
            return 31 * (31 * (usedChanges.hashValues(hashValues) * 31 + hash) + changed.hashCode()) + (propClasses?1:0);
        }

        public QuickSet<Value> getValues() {
            return AbstractOuterContext.getOuterValues(values.values()).merge(usedChanges.getContextValues());
        }

        QueryInterfaceImplement(QueryInterfaceImplement<K> implement, MapValuesTranslate translator) {
            usedChanges = implement.usedChanges.translateValues(translator);
            this.changed = implement.changed;
            this.propClasses = implement.propClasses;
            this.values = new MapTranslator(new HashMap<KeyExpr, KeyExpr>(), translator).translate(implement.values);  // assert что keys'ов нет
        }

        protected QueryInterfaceImplement<K> translate(MapValuesTranslate translator) {
            return new QueryInterfaceImplement<K>(this, translator);
        }
    }

    public <K extends PropertyInterface> IQuery<K, String> getQuery(CalcProperty<K> property, boolean propClasses, PropertyChanges propChanges, PropertyQueryType queryType, Map<K, ? extends Expr> interfaceValues, Map<Integer, Collection<CacheResult<QueryInterfaceImplement<K>, ValuesContext>>> exprCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {
        // assert что в interfaceValues только values

        if(disableCaches)
            return (IQuery<K, String>) thisJoinPoint.proceed();

        property.cached = true;

        QueryInterfaceImplement<K> implement = new QueryInterfaceImplement<K>(property,propClasses,propChanges,queryType,interfaceValues);

        Collection<CacheResult<QueryInterfaceImplement<K>, ValuesContext>> hashCaches;
        synchronized(exprCaches) {
            int hashImplement = implement.getValueComponents().hash;
            hashCaches = exprCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new ArrayList<CacheResult<QueryInterfaceImplement<K>, ValuesContext>>();
                exprCaches.put(hashImplement, hashCaches);
            }
        }

        ValuesContext query = null;
        ValuesContext cacheQuery = null;
        synchronized(hashCaches) {
            for(CacheResult<QueryInterfaceImplement<K>, ValuesContext> cache : hashCaches) {
                for(MapValuesTranslate mapValues : new MapValuesIterable(cache.implement, implement)) {
                    if(cache.implement.translateValues(mapValues).equals(implement)) {
                        ValuesContext<?> cacheResult = (ValuesContext<?>) cache.result;
                        cacheQuery = cacheResult.translateValues(mapValues.filter(cacheResult.getContextValues())); // так как могут не использоваться values в Query
                        break;
                    }
                }
            }
            if(cacheQuery==null || checkCaches) {
                query = (IQuery<K, String>) thisJoinPoint.proceed();

                assert implement.getContextValues().containsAll(ValueExpr.removeStatic(query.getContextValues().getSet())); // в query не должно быть элементов не из implement.getContextValues

                if(!(checkCaches && cacheQuery!=null))
                    cacheNoBig(implement, hashCaches, query);

                logger.debug("getExpr - not cached "+property);
            } else {
                query = cacheQuery;

                logger.debug("getExpr - cached "+property);
            }
        }

        if (checkCaches && cacheQuery!=null && !BaseUtils.hashEquals(query, cacheQuery))
            query = query;
        return (IQuery<K, String>)query;
    }

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("execution(* platform.server.logics.property.CalcProperty.getQuery(boolean,platform.server.session.PropertyChanges,platform.server.logics.property.PropertyQueryType,java.util.Map)) " +
            "&& target(property) && args(propClasses,propChanges,queryType,interfaceValues)")
    public Object callGetQuery(ProceedingJoinPoint thisJoinPoint, CalcProperty property, boolean propClasses, PropertyChanges propChanges, PropertyQueryType queryType, Map interfaceValues) throws Throwable {
        // сначала target в аспекте должен быть
        return getQuery(property, propClasses, propChanges, queryType, interfaceValues, ((MapPropertyInterface) property).getJoinExprCache(), thisJoinPoint);
    }

    public static class JoinExprInterfaceImplement<P extends PropertyInterface> extends AbstractInnerContext<JoinExprInterfaceImplement<P>> {
        private final PropertyChanges usedChanges;
        private final Map<P, Expr> joinImplement;
        private final boolean where;
        private final boolean propClasses;

        public JoinExprInterfaceImplement(CalcProperty<P> property, Map<P, Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, boolean where) {
            usedChanges = property.getUsedChanges(propChanges);
            this.propClasses = propClasses;
            this.joinImplement = joinImplement;
            this.where = where;
        }

        public boolean equalsInner(JoinExprInterfaceImplement<P> o) {
            return BaseUtils.hashEquals(joinImplement,o.joinImplement) && BaseUtils.hashEquals(usedChanges,o.usedChanges) && where == o.where && propClasses == o.propClasses;
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(HashContext hashContext) {
            return 31 * usedChanges.hashValues(hashContext.values) + AbstractOuterContext.hashOuter(joinImplement, hashContext) + (where?1:0) + (propClasses?5:0);
        }

        public QuickSet<KeyExpr> getKeys() {
            return AbstractOuterContext.getOuterKeys(joinImplement.values());
        }

        public QuickSet<Value> getValues() {
            return AbstractOuterContext.getOuterValues(joinImplement.values()).merge(usedChanges.getContextValues());
        }

        private JoinExprInterfaceImplement(JoinExprInterfaceImplement<P> implement, MapTranslate translator) {
            usedChanges = implement.usedChanges.translate(translator.mapValues());
            joinImplement = translator.translate(implement.joinImplement);
            this.where = implement.where;
            this.propClasses = implement.propClasses;
        }

        public JoinExprInterfaceImplement<P> translate(MapTranslate translator) {
            return new JoinExprInterfaceImplement<P>(this, translator);
        }
    }

    static class ExprResult extends AbstractTranslateValues<ExprResult> {
        final Expr expr;
        final Where where;

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

    public <K extends PropertyInterface> Expr getJoinExpr(CalcProperty<K> property, Map<K, Expr> joinExprs, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWheres, Map<Integer, Collection<CacheResult<JoinExprInterfaceImplement, ExprResult>>> exprCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        // здесь по идее на And не надо проверять
        if(disableCaches || property instanceof FormulaProperty)
            return (Expr) thisJoinPoint.proceed();

        property.cached = true;

        JoinExprInterfaceImplement<K> implement = new JoinExprInterfaceImplement<K>(property,joinExprs,propClasses,propChanges,changedWheres!=null);

        Collection<CacheResult<JoinExprInterfaceImplement, ExprResult>> hashCaches;
        synchronized(exprCaches) {
            int hashImplement = implement.getInnerComponents(true).hash;
            hashCaches = exprCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = new ArrayList<CacheResult<JoinExprInterfaceImplement, ExprResult>>();
                exprCaches.put(hashImplement, hashCaches);
            }
        }

        synchronized(hashCaches) {
            Expr cacheResult = null;
            for(CacheResult<JoinExprInterfaceImplement, ExprResult> cache : hashCaches) {
                MapTranslate translator;
                if((translator=cache.implement.mapInner(implement, true))!=null) {
                    logger.debug("getExpr - cached "+property);
                    if(changedWheres!=null) changedWheres.add(cache.result.where.translateOuter(translator));
                    cacheResult = cache.result.expr.translateOuter(translator);
                    if(checkCaches)
                        break;
                    else
                        return cacheResult;
                }
            }

            logger.debug("getExpr - not cached "+property);
            WhereBuilder cacheWheres = CalcProperty.cascadeWhere(changedWheres);
            Expr expr = (Expr) thisJoinPoint.proceed(new Object[]{property, joinExprs, propClasses, propChanges, cacheWheres});

            cacheNoBig(implement, hashCaches, new ExprResult(expr, changedWheres != null ? cacheWheres.toWhere() : null));
            if(checkCaches && !BaseUtils.hashEquals(expr, cacheResult))
                expr = expr;

            // проверим
            if(checkInfinite)
                expr.checkInfiniteKeys();

            if(changedWheres!=null) changedWheres.add(cacheWheres.toWhere());
            return expr;
        }
    }

    public static boolean checkInfinite = false;

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("execution(* platform.server.logics.property.CalcProperty.getJoinExpr(java.util.Map,boolean,platform.server.session.PropertyChanges,platform.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,propClasses,propChanges,changedWhere)")
    public Object callGetJoinExpr(ProceedingJoinPoint thisJoinPoint, CalcProperty property, Map joinExprs, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        // сначала target в аспекте должен быть
        return getJoinExpr(property, joinExprs, propClasses, propChanges, changedWhere, ((MapPropertyInterface) property).getExprCache(), thisJoinPoint);
    }
}
