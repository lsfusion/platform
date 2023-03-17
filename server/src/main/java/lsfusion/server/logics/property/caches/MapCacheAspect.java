package lsfusion.server.logics.property.caches;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddCol;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSSVSMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.CacheAspect;
import lsfusion.server.base.caches.CacheStats.CacheType;
import lsfusion.server.data.AbstractSourceJoin;
import lsfusion.server.data.caches.*;
import lsfusion.server.data.caches.hash.HashCodeKeys;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.caches.hash.HashValues;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.query.translate.MapJoin;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.translate.*;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.DataChanges;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.change.StructChanges;
import lsfusion.server.logics.action.session.change.modifier.SessionModifier;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyQueryType;
import lsfusion.server.logics.property.classes.data.FormulaProperty;
import lsfusion.server.logics.property.classes.infer.CalcClassType;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import lsfusion.server.physics.exec.hint.AutoHintsAspect;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Arrays;

import static lsfusion.server.base.caches.CacheStats.incrementHit;
import static lsfusion.server.base.caches.CacheStats.incrementMissed;

@Aspect
public class MapCacheAspect {
    private final static Logger logger = Logger.getLogger(MapCacheAspect.class);

    public static class CacheResult<I extends ValuesContext, R extends TranslateValues> {
        public final I implement;
        public final R result;

        private CacheResult(I implement, R result) {
            this.implement = implement;
            this.result = result;
        }
    }

    class JoinImplement<K> extends AbstractInnerContext<JoinImplement<K>> {
        final ImMap<K,? extends Expr> exprs;
        final MapValuesTranslate mapValues; // map context'а values на те которые нужны

        JoinImplement(ImMap<K, ? extends Expr> exprs,MapValuesTranslate mapValues) {
            this.exprs = exprs;
            this.mapValues = mapValues;
        }

        public ImSet<ParamExpr> getKeys() {
            return AbstractOuterContext.getOuterColKeys(exprs.values());
        }

        public ImSet<Value> getValues() {
            // нельзя из values так как вообще не его контекст
            return AbstractOuterContext.getOuterColValues(exprs.values()).merge(mapValues.getValues());
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(HashContext hashContext) {
            return AbstractSourceJoin.hashOuter(exprs, hashContext) * 31 + mapValues.hash(hashContext.values);
        }

        protected JoinImplement<K> translate(MapTranslate translate) {
            return new JoinImplement<>(translate.translate(exprs), mapValues.mapTrans(translate.mapValues()));
        }

        public boolean equalsInner(JoinImplement<K> object) {
            return BaseUtils.hashEquals(exprs,object.exprs) && BaseUtils.hashEquals(mapValues,object.mapValues);
        }
    }
    
    public enum Type {
        JOIN, DATACHANGES, QUERY, JOINEXPR, INCCHANGE, READSAVE, AUTOHINT
    }
    private static LRUWSSVSMap<Object, Integer, Type, MAddCol<CacheResult>> mapCaches = new LRUWSSVSMap<>(LRUUtil.G2);
    public static <K extends ValuesContext, V extends TranslateValues> MAddCol<CacheResult<K, V>> getCachedCol(Object object, Integer mapParamHash, Type type) {
        MAddCol<CacheResult> cacheCol = mapCaches.get(object, mapParamHash, type);
        if(cacheCol == null) {
            cacheCol = ListFact.mAddCol();
            mapCaches.put(object, mapParamHash, type, cacheCol);                    
        }
        return (MAddCol)cacheCol;            
    }
    public static void cleanClassCaches() {
        mapCaches.proceedSafeLockLRUEKeyValues((type, caches) -> {
            if (type == Type.QUERY || type == Type.JOINEXPR) {
                synchronized (caches) {
                    for (int i = caches.size() - 1; i >= 0; i--) {
                        CacheResult<CalcTypeImplement, ?> cache = caches.get(i);
                        if (cache.implement.getCalcType() instanceof CalcClassType)
                            caches.remove(i);
                    }
                }
            }
        });        
    }

    private <K,V> Join<V> join(Query<K, V> query, ImMap<K, ? extends Expr> joinExprs, MapValuesTranslate joinValues, ProceedingJoinPoint thisJoinPoint) throws Throwable {
//        assert BaseUtils.onlyObjects(joinExprs.keySet()); он вообщем то не нужен, так как hashCaches хранится для Query, а он уже хранит K
        assert ((QueryCacheAspect.QueryCacheInterface)query).getCacheTwin() == query;

        JoinImplement<K> joinImplement = new JoinImplement<>(joinExprs, joinValues);

        MAddCol<CacheResult<JoinImplement<K>, Join<V>>> hashCaches = getCachedCol(query, joinImplement.getInnerComponents(true).hash, MapCacheAspect.Type.JOIN);
        synchronized(hashCaches) {
            for(CacheResult<JoinImplement<K>, Join<V>> cache : hashCaches.it()) {
                MapTranslate translator;
                if((translator = cache.implement.mapInner(joinImplement, true))!=null) {
                    // здесь не все values нужно докинуть их из контекста (ключи по идее все)
                    logger.debug("join cached");
                    incrementHit(CacheType.JOIN);
                    return new MapJoin<>(translator, cache.result);
                }
            }
            incrementMissed(CacheType.JOIN);
/*            logger.info("join not cached"); //можно было бы так, уже есть translateRemoveValues, но так по аналогии с Query будут сразу кэшироваться нужные вызовы
            Join<V> join = (Join<V>) thisJoinPoint.proceed();
            cacheNoBig(joinImplement, hashCaches, join);
            return join;*/

            // такой вариант предпочтительнее с точки зрения кэшей, но в большинстве случаев сделан без него
            ImRevMap<Value, Value> bigValues = AbstractValuesContext.getBigValues(joinImplement.getContextValues());
            if(bigValues == null) {
                Join<V> join = (Join<V>) thisJoinPoint.proceed();
                hashCaches.add(new CacheResult<>(joinImplement, join));
                return join;
            } else { // для предотвращения утечки памяти, bigvalues - работа с транслированными объектами, а в конце трансляция назад
                JoinImplement<K> cacheImplement = joinImplement.translateInner(new RemapValuesTranslator(bigValues));

                Join<V> join = (Join<V>) thisJoinPoint.proceed(new Object[]{query, cacheImplement.exprs, cacheImplement.mapValues});
                hashCaches.add(new CacheResult<>(cacheImplement, join));

                return new MapJoin<>((MapTranslate) new RemapValuesTranslator(bigValues.reverse()), join);
            }
        }
    }

    @Around("execution(lsfusion.server.data.query.build.Join lsfusion.server.data.query.Query.joinExprs(lsfusion.base.col.interfaces.immutable.ImMap,lsfusion.server.data.translate.MapValuesTranslate)) && target(query) && args(joinExprs,mapValues)")
    public Object callJoin(ProceedingJoinPoint thisJoinPoint, Query query, ImMap joinExprs, MapValuesTranslate mapValues) throws Throwable {
        return join(query, joinExprs, mapValues, thisJoinPoint);
    }

    public static boolean checkCaches = false;
    public static boolean checkCaches() {
        return checkCaches || ServerLoggers.isUserExLog();
    }

    public static <T> void logCaches(T cached, T calced, ProceedingJoinPoint jp, String action, Property property) {
        boolean match = BaseUtils.hashEquals(cached, calced);
        ServerLoggers.hExInfoLogger.info(jp.getThis() + " " + action + " " + (match ? " MATCH " : "NOMATCH CACHED : " + cached + " CALCED: ") + calced + " ARGS " + Arrays.toString(jp.getArgs()) + (property != null ? "DEP " + property.getRecDepends() : "" ) );
    }

    // needed for check caches
    private static ThreadLocal<Boolean> recursiveUsedChanges = new ThreadLocal<>();

    public <K extends PropertyInterface> ImSet<Property> getUsedChanges(Property<K> property, StructChanges implement, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        if(!(property instanceof AggregateProperty) && !(property instanceof DataProperty && ((DataProperty) property).event!=null)) // если не Function или DataProperty с derived, то нету рекурсии и эффективнее просто вы
            return (ImSet<Property>) thisJoinPoint.proceed();

        final boolean checkCaches = checkCaches();
        boolean inRecursion = false;

        if(checkCaches) {
            inRecursion = recursiveUsedChanges.get() != null;
            recursiveUsedChanges.set(true);
        }

        // оптимизация самого верхнего уровня, проверка на isEmpty нужна для того чтобы до finalize'a (в CaseUnionProperty в частности) не вызвать случайно getRecDepends, там в fillDepends на это assert есть
        final Object[] filteredArgs = {!implement.isEmpty() ? implement.filterForProperty(property) : implement};
        final ImSet<Property> result = (ImSet<Property>) CacheAspect.lazyIdentityExecute(property, thisJoinPoint, filteredArgs, true, CacheAspect.Type.SIMPLE, CacheType.USED_CHANGES);

        if(checkCaches) {
            if(!inRecursion) {
                final Object notFilteredResult = thisJoinPoint.proceed();
                logCaches(result, notFilteredResult, thisJoinPoint, "USEDCHANGES", property);

                recursiveUsedChanges.set(null);
            }
        }

        return result;
    }

    @Around("execution(* lsfusion.server.logics.property.Property.getUsedChanges(lsfusion.server.logics.action.session.change.StructChanges)) " +
            "&& target(property) && args(changes)")
    public Object callGetUsedChanges(ProceedingJoinPoint thisJoinPoint, Property property, StructChanges changes) throws Throwable {
        return getUsedChanges(property, changes, thisJoinPoint);
    }

    public <K extends PropertyInterface> boolean aspectHasPreread(Property<K> property, StructChanges implement, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        assert property.hasGlobalPreread();

        // the same as in getUsedChanges
        final Object[] filteredArgs = {!implement.isEmpty() ? implement.filterForProperty(property) : implement};
        boolean result = (boolean) CacheAspect.lazyIdentityExecute(property, thisJoinPoint, filteredArgs, true, CacheAspect.Type.SIMPLE, CacheType.HAS_PREREAD);

        if(checkCaches()) {
            final Object notFilteredResult = thisJoinPoint.proceed();
            logCaches(result, notFilteredResult, thisJoinPoint, "HASPREREAD", property);
        }

        return result;
    }

    @Around("execution(* lsfusion.server.logics.property.Property.aspectHasPreread(lsfusion.server.logics.action.session.change.StructChanges)) " +
            "&& target(property) && args(changes)")
    public Object callAspectHasPreread(ProceedingJoinPoint thisJoinPoint, Property property, StructChanges changes) throws Throwable {
        return aspectHasPreread(property, changes, thisJoinPoint);
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

        public ImSet<ParamExpr> getKeys() {
            return change.getInnerKeys();
        }

        public ImSet<Value> getValues() {
            return usedChanges.getContextValues().merge(change.getInnerValues());
        }

        DataChangesInterfaceImplement(DataChangesInterfaceImplement<P> implement, MapTranslate translator) {
            usedChanges = implement.usedChanges.translateValues(translator.mapValues());
            change = implement.change.translateInner(translator);
            this.where = implement.where;
        }

        protected DataChangesInterfaceImplement<P> translate(MapTranslate translator) {
            return new DataChangesInterfaceImplement<>(this, translator);
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
            return new DataChangesResult<>(changes.translateValues(translate), where == null ? null : where.translateOuter(translate.mapKeys()));
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return changes.equals(((DataChangesResult<P>)o).changes) && BaseUtils.nullEquals(where,((DataChangesResult<P>)o).where);
        }

        public int immutableHashCode() {
            return changes.hashCode() * 31 + BaseUtils.nullHash(where);
        }
    }

    public <K extends PropertyInterface> DataChanges getDataChanges(Property<K> property, PropertyChange<K> change, WhereBuilder changedWheres, PropertyChanges propChanges, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        if(disableCaches)
            return (DataChanges) thisJoinPoint.proceed();

        DataChangesInterfaceImplement<K> implement = new DataChangesInterfaceImplement<>(property, change, propChanges, changedWheres != null);

        MAddCol<CacheResult<DataChangesInterfaceImplement<K>, DataChangesResult<K>>> hashCaches = getCachedCol(property, implement.getInnerComponents(true).hash, MapCacheAspect.Type.DATACHANGES);
        synchronized(hashCaches) {
            for(CacheResult<DataChangesInterfaceImplement<K>, DataChangesResult<K>> cache : hashCaches.it()) {
                MapTranslate translator;
                if((translator=cache.implement.mapInner(implement, true))!=null) {
                    logger.debug("getDataChanges - cached "+property);
                    incrementHit(CacheType.DATA_CHANGES);
                    if(changedWheres!=null) changedWheres.add(cache.result.where.translateOuter(translator));
                    return cache.result.changes.translateValues(translator.mapValues());
                }
            }

            logger.debug("getDataChanges - not cached "+property);
            incrementMissed(CacheType.DATA_CHANGES);
            WhereBuilder cacheWheres = Property.cascadeWhere(changedWheres);
            DataChanges changes = (DataChanges) thisJoinPoint.proceed(new Object[]{property, change, propChanges, cacheWheres});

            cacheNoBig(implement, hashCaches, new DataChangesResult<>(changes, changedWheres != null ? cacheWheres.toWhere() : null));

            if(changedWheres!=null) changedWheres.add(cacheWheres.toWhere());
            return changes;
        }
    }

    public static <I extends ValuesContext<I>, R extends TranslateValues<R>> void cacheNoBig(I implement, MAddCol<CacheResult<I, R>> hashCaches, R result) {
        ImRevMap<Value, Value> bigValues = AbstractValuesContext.getBigValues(implement.getContextValues());
        if(bigValues == null) // если нет больших значений просто записываем
            hashCaches.add(new CacheResult<>(implement, result));
        else { // bigvalues - работа со старыми объектами, а сохранение транслированных
            MapValuesTranslator removeBig = new RemapValuesTranslator(bigValues);
            hashCaches.add(new CacheResult<>(implement.translateRemoveValues(removeBig), result.translateRemoveValues(removeBig)));
        }
    }

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("execution(lsfusion.server.logics.action.session.change.DataChanges lsfusion.server.logics.property.Property.getDataChanges(lsfusion.server.logics.action.session.change.PropertyChange,lsfusion.server.logics.action.session.change.PropertyChanges,lsfusion.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(change,propChanges,changedWhere)")
    public Object callGetDataChanges(ProceedingJoinPoint thisJoinPoint, Property property, PropertyChange change, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        // сначала target в аспекте должен быть
        return getDataChanges(property, change, changedWhere, propChanges, thisJoinPoint);
    }

    private interface CalcTypeImplement<This extends ValuesContext<This>> extends ValuesContext<This> {
        CalcType getCalcType();
    }
    
    public static class QueryInterfaceImplement<K extends PropertyInterface> extends AbstractValuesContext<QueryInterfaceImplement<K>> implements CalcTypeImplement<QueryInterfaceImplement<K>> {
        public final PropertyChanges usedChanges;
        private final PropertyQueryType changed; // нужно ли условие на изменение, по сути для этого св-ва и делается класс
        private final ImMap<K, ? extends Expr> values;
        private final CalcType calcType;

        public CalcType getCalcType() {
            return calcType;
        }

        private boolean cachedPrereadHintEnabled; // transient

        QueryInterfaceImplement(Property<?> property, CalcType calcType, PropertyChanges changes, PropertyQueryType changed, ImMap<K, ? extends Expr> values) {
            usedChanges = property.getUsedChanges(changes);
            this.calcType = calcType;
            this.changed = (changed == PropertyQueryType.RECURSIVE ? PropertyQueryType.CHANGED : changed);
            this.values = values;
        }

        @Override
        public boolean calcTwins(TwinImmutableObject o) {
            return changed == ((QueryInterfaceImplement<K>)o).changed && calcType.equals(((QueryInterfaceImplement<K>)o).calcType) && usedChanges.equals(((QueryInterfaceImplement<K>)o).usedChanges) && values.equals(((QueryInterfaceImplement<K>)o).values);
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(HashValues hashValues) {
            int hash = 0;
            for(int i=0,size=values.size();i<size;i++)
                hash += values.getKey(i).hashCode() ^ values.getValue(i).hashOuter(HashContext.create(HashCodeKeys.instance, hashValues));
            return 31 * (31 * (usedChanges.hashValues(hashValues) * 31 + hash) + changed.hashCode()) + calcType.hashCode();
        }

        public ImSet<Value> getValues() {
            return AbstractOuterContext.getOuterColValues(values.values()).merge(usedChanges.getContextValues());
        }

        QueryInterfaceImplement(QueryInterfaceImplement<K> implement, MapValuesTranslate translator) {
            usedChanges = implement.usedChanges.translateValues(translator);
            this.changed = implement.changed;
            this.calcType = implement.calcType;
            this.values = translator.mapKeys().translate(implement.values);  // assert что keys'ов нет
        }

        protected QueryInterfaceImplement<K> translate(MapValuesTranslate translator) {
            return new QueryInterfaceImplement<>(this, translator);
        }
    }

    public <K extends PropertyInterface> IQuery<K, String> getQuery(Property<K> property, CalcType calcType, PropertyChanges propChanges, PropertyQueryType queryType, ImMap<K, ? extends Expr> interfaceValues, ProceedingJoinPoint thisJoinPoint) throws Throwable {
        // assert что в interfaceValues только values

        final boolean checkCaches = MapCacheAspect.checkCaches();

        if(disableCaches)
            return (IQuery<K, String>) thisJoinPoint.proceed();

        QueryInterfaceImplement<K> implement = new QueryInterfaceImplement<>(property, calcType, propChanges, queryType, interfaceValues);

        ValuesContext query = null;
        ValuesContext cacheQuery = null;
        QueryInterfaceImplement<K> cachedImplement = null;
        MAddCol<CacheResult<QueryInterfaceImplement<K>, ValuesContext>> hashCaches = getCachedCol(property, implement.getValueComponents().hash, MapCacheAspect.Type.QUERY);
        synchronized(hashCaches) {
            for(CacheResult<QueryInterfaceImplement<K>, ValuesContext> cache : hashCaches.it()) {
                for(MapValuesTranslate mapValues : new MapValuesIterable(cache.implement, implement)) {
                    if(cache.implement.translateValues(mapValues).equals(implement)) {
                        if(!cache.implement.cachedPrereadHintEnabled && prereadHintEnabled(property, implement.usedChanges, calcType)) { // нашли кэш, но он не подходит, так как могут быть hint'ы
                            cachedImplement = cache.implement;
                            break;
                        }

                        ValuesContext<?> cacheResult = (ValuesContext<?>) cache.result;
                        cacheQuery = cacheResult.translateValues(mapValues.filter(cacheResult.getContextValues())); // так как могут не использоваться values в Query
                        break;
                    }
                }
            }
            if(cacheQuery==null || checkCaches) {
                query = (IQuery<K, String>) thisJoinPoint.proceed(new Object[] {property, calcType, implement.usedChanges, queryType, interfaceValues} );

                assert implement.getContextValues().containsAll(ValueExpr.removeStatic(query.getContextValues())); // в query не должно быть элементов не из implement.getContextValues

                if(cacheQuery != null)
                    logCaches(((IQuery<K, String>)cacheQuery).getQuery(), query, thisJoinPoint, "QUERY", property);

                if(cachedImplement == null) { // если кэша не было
                    if(!(checkCaches && cacheQuery!=null)) {
                        cacheNoBig(implement, hashCaches, query);
                    }
                    implement.cachedPrereadHintEnabled = prereadHintEnabled(property, implement.usedChanges, calcType);
                } else
                    cachedImplement.cachedPrereadHintEnabled = true;

                logger.debug("getExpr - not cached "+property);
                incrementMissed(CacheType.EXPR);
            } else {
                query = cacheQuery;

                logger.debug("getExpr - cached "+property);
                incrementHit(CacheType.EXPR);
            }
        }

        if (checkCaches && cacheQuery!=null && !BaseUtils.hashEquals(query, cacheQuery))
            query = query;
        return (IQuery<K, String>)query;
    }

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("execution(* lsfusion.server.logics.property.Property.getQuery(lsfusion.server.logics.property.CalcType,lsfusion.server.logics.action.session.change.PropertyChanges,lsfusion.server.logics.property.PropertyQueryType,lsfusion.base.col.interfaces.immutable.ImMap)) " +
            "&& target(property) && args(calcType,propChanges,queryType,interfaceValues)")
    public Object callGetQuery(ProceedingJoinPoint thisJoinPoint, Property property, CalcType calcType, PropertyChanges propChanges, PropertyQueryType queryType, ImMap interfaceValues) throws Throwable {
        // сначала target в аспекте должен быть
        return getQuery(property, calcType, propChanges, queryType, interfaceValues, thisJoinPoint);
    }

    public static class JoinExprInterfaceImplement<P extends PropertyInterface> extends AbstractInnerContext<JoinExprInterfaceImplement<P>> implements CalcTypeImplement<JoinExprInterfaceImplement<P>> {
        public final PropertyChanges usedChanges;
        private final ImMap<P, Expr> joinImplement;
        private final boolean where;
        private final CalcType calcType;

        public CalcType getCalcType() {
            return calcType;
        }
        
        private boolean cachedPrereadHintEnabled; // transient

        public JoinExprInterfaceImplement(Property<P> property, ImMap<P, Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, boolean where) {
            usedChanges = property.getUsedChanges(propChanges);
            this.calcType = calcType;
            this.joinImplement = joinImplement;
            this.where = where;
        }

        public boolean equalsInner(JoinExprInterfaceImplement<P> o) {
            return BaseUtils.hashEquals(joinImplement,o.joinImplement) && BaseUtils.hashEquals(usedChanges,o.usedChanges) && where == o.where && calcType.equals(o.calcType);
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(HashContext hashContext) {
            return 31 * usedChanges.hashValues(hashContext.values) + AbstractOuterContext.hashOuter(joinImplement, hashContext) + (where?1:0) + calcType.hashCode();
        }

        public ImSet<ParamExpr> getKeys() {
            return AbstractOuterContext.getOuterColKeys(joinImplement.values());
        }

        public ImSet<Value> getValues() {
            return AbstractOuterContext.getOuterColValues(joinImplement.values()).merge(usedChanges.getContextValues());
        }

        private JoinExprInterfaceImplement(JoinExprInterfaceImplement<P> implement, MapTranslate translator) {
            usedChanges = implement.usedChanges.translateValues(translator.mapValues());
            joinImplement = translator.translate(implement.joinImplement);
            this.where = implement.where;
            this.calcType = implement.calcType;
        }

        public JoinExprInterfaceImplement<P> translate(MapTranslate translator) {
            return new JoinExprInterfaceImplement<>(this, translator);
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

        public boolean calcTwins(TwinImmutableObject o) {
            return expr.equals(((ExprResult)o).expr) && BaseUtils.nullEquals(where, ((ExprResult)o).where);
        }

        public int immutableHashCode() {
            return expr.hashCode() * 31 + BaseUtils.nullHash(where);
        }
    }

    public static boolean disableCaches = false;
    
    // has to be consistent с SessionModifier.allowPrereadValues
    // we need this check to avoid using caches, that where calculated when hints were disabled
    private static boolean prereadHintEnabled(Property property, PropertyChanges propertyChanges, CalcType calcType) {
        return !calcType.isExpr() || modifierHasPrereadAndIsNotPreread() || !property.hasPreread(propertyChanges);
    }

    private static boolean modifierHasPrereadAndIsNotPreread() {
        SessionModifier modifier = AutoHintsAspect.catchAutoHint.get();
        return modifier != null && modifier.prereadProps.isEmpty(); // if there is SessionModifier and it's not preread execution allow using cache (it is assumed that all other parameters used in allowPrereadValues are alread included in the cache key)
    }

    public <K extends PropertyInterface> Expr getJoinExpr(Property<K> property, ImMap<K, Expr> joinExprs, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWheres, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        final boolean checkCaches = MapCacheAspect.checkCaches();

        // здесь по идее на And не надо проверять
        if(disableCaches || property instanceof FormulaProperty)
            return (Expr) thisJoinPoint.proceed();

        JoinExprInterfaceImplement<K> implement = new JoinExprInterfaceImplement<>(property, joinExprs, calcType, propChanges, changedWheres != null);

        JoinExprInterfaceImplement<K> cachedImplement = null;
        MAddCol<CacheResult<JoinExprInterfaceImplement<K>, ExprResult>> hashCaches = getCachedCol(property, implement.getInnerComponents(true).hash, MapCacheAspect.Type.JOINEXPR);
        synchronized(hashCaches) {
            Expr cacheResult = null;
            for(CacheResult<JoinExprInterfaceImplement<K>, ExprResult> cache : hashCaches.it()) {
                MapTranslate translator;
                if((translator=cache.implement.mapInner(implement, true))!=null) {
                    if(!cache.implement.cachedPrereadHintEnabled && prereadHintEnabled(property, implement.usedChanges, calcType)) { // нашли кэш, но он не подходит, так как могут быть hint'ы
                        cachedImplement = cache.implement;
                        break;
                    }
                        
                    logger.debug("getExpr - cached "+property);
                    incrementHit(CacheType.JOIN_EXPR);
                    if(changedWheres!=null) changedWheres.add(cache.result.where.translateOuter(translator));
                    cacheResult = cache.result.expr.translateOuter(translator);
                    if(checkCaches)
                        break;
                    else
                        return cacheResult;
                }
            }

            incrementMissed(CacheType.JOIN_EXPR);
            WhereBuilder cacheWheres = Property.cascadeWhere(changedWheres);
            Expr expr = (Expr) thisJoinPoint.proceed(new Object[]{property, joinExprs, calcType, implement.usedChanges, cacheWheres});

            logger.debug("getExpr - not cached "+property);
            if(cachedImplement == null) { // если кэша не было
                cacheNoBig(implement, hashCaches, new ExprResult(expr, changedWheres != null ? cacheWheres.toWhere() : null));
                implement.cachedPrereadHintEnabled = prereadHintEnabled(property, implement.usedChanges, calcType);
            } else
                cachedImplement.cachedPrereadHintEnabled = true;
            if(checkCaches && cacheResult != null)
                logCaches(cacheResult, expr, thisJoinPoint, "JOINEXPR", property);

            // проверим
            if(checkInfinite && property.isFull(calcType.getAlgInfo()))
                expr.checkInfiniteKeys();

            if(changedWheres!=null) changedWheres.add(cacheWheres.toWhere());
            return expr;
        }
    }

    public static boolean checkInfinite = false;

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("execution(* lsfusion.server.logics.property.Property.getJoinExpr(lsfusion.base.col.interfaces.immutable.ImMap,lsfusion.server.logics.property.CalcType,lsfusion.server.logics.action.session.change.PropertyChanges,lsfusion.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,calcType,propChanges,changedWhere)")
    public Object callGetJoinExpr(ProceedingJoinPoint thisJoinPoint, Property property, ImMap joinExprs, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        // сначала target в аспекте должен быть
        return getJoinExpr(property, joinExprs, calcType, propChanges, changedWhere, thisJoinPoint);
    }

    public <K extends PropertyInterface> PropertyChange<K> getIncrementChange(Property<K> property, PropertyChanges propChanges, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        final boolean checkCaches = MapCacheAspect.checkCaches();

        // assert что в interfaceValues только values
        if(disableCaches)
            return (PropertyChange<K>) thisJoinPoint.proceed();

        PropertyChanges implement = property.getUsedChanges(propChanges);

        PropertyChange<K> change;
        PropertyChange<K> cacheChange = null;
        MAddCol<CacheResult<PropertyChanges, PropertyChange<K>>> hashCaches = getCachedCol(property, implement.getValueComponents().hash, MapCacheAspect.Type.INCCHANGE);
        synchronized(hashCaches) {
            for(CacheResult<PropertyChanges, PropertyChange<K>> cache : hashCaches.it()) {
                for(MapValuesTranslate mapValues : new MapValuesIterable(cache.implement, implement)) {
                    if(cache.implement.translateValues(mapValues).equals(implement)) {
                        cacheChange = cache.result.translateValues(mapValues.filter(cache.result.getInnerValues())); // так как могут не использоваться values в Query
                        break;
                    }
                }
            }
            if(cacheChange==null || checkCaches) {
                change = (PropertyChange<K>) thisJoinPoint.proceed(new Object[]{property, implement});

                assert implement.getContextValues().containsAll(ValueExpr.removeStatic(change.getInnerValues())); // в query не должно быть элементов не из implement.getContextValues

                if(cacheChange == null)
                    cacheNoBig(implement, hashCaches, change);
                else
                    logCaches(cacheChange, change, thisJoinPoint, "INCREMENTCHANGE", property);

                logger.info("getIncrementChange - not cached "+property);
                incrementMissed(CacheType.INCREMENT_CHANGE);
            } else {
                change = cacheChange;

                logger.info("getIncrementChange - cached "+property);
                incrementHit(CacheType.INCREMENT_CHANGE);
            }
        }

        if (checkCaches && cacheChange!=null && !BaseUtils.hashEquals(change, cacheChange))
            change = change;
        return change;
    }

    // aspect нужен по большому счету чтобы сохранять query, и не делать лишних getInnerComponents
    @Around("execution(* lsfusion.server.logics.property.Property.getIncrementChange(lsfusion.server.logics.action.session.change.PropertyChanges)) " +
            "&& target(property) && args(propChanges)")
    public Object callGetIncrementChange(ProceedingJoinPoint thisJoinPoint, Property property, PropertyChanges propChanges) throws Throwable {
        return getIncrementChange(property, propChanges, thisJoinPoint);
    }

    // read save cache
    static class ReadSaveInterfaceImplement extends AbstractValuesContext<ReadSaveInterfaceImplement> {
        private final ImSet<Property> properties;
        private final PropertyChanges usedChanges;

        ReadSaveInterfaceImplement(ImSet<Property> properties, PropertyChanges changes) {
            MSet<Property> mCommonChanges = SetFact.mSet();
            for(Property property : properties)
                mCommonChanges.addAll(property.getSetUsedChanges(changes));
            usedChanges = changes.filter(mCommonChanges.immutable());
            this.properties = properties;
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return BaseUtils.hashEquals(properties,((ReadSaveInterfaceImplement)o).properties) && BaseUtils.hashEquals(usedChanges,((ReadSaveInterfaceImplement)o).usedChanges);
        }

        protected boolean isComplex() {
            return true;
        }

        public int hash(HashValues hash) {
            return 31 * usedChanges.hashValues(hash) + properties.hashCode();
        }

        public ImSet<Value> getValues() {
            return usedChanges.getContextValues();
        }

        private ReadSaveInterfaceImplement(ReadSaveInterfaceImplement implement, MapValuesTranslate translator) {
            usedChanges = implement.usedChanges.translateValues(translator);
            properties = implement.properties;
        }

        protected ReadSaveInterfaceImplement translate(MapValuesTranslate translator) {
            return new ReadSaveInterfaceImplement(this, translator);
        }
    }

    public <K extends PropertyInterface> IQuery<KeyField, Property> getReadSaveQuery(ImplementTable table, ImSet<Property> properties, PropertyChanges propChanges, ProceedingJoinPoint thisJoinPoint) throws Throwable {
        final boolean checkCaches = MapCacheAspect.checkCaches();

        // assert что в interfaceValues только values
        if(disableCaches)
            return (IQuery<KeyField, Property>) thisJoinPoint.proceed();

        ReadSaveInterfaceImplement implement = new ReadSaveInterfaceImplement(properties, propChanges);

        IQuery<KeyField, Property> query;
        IQuery<KeyField, Property> cacheQuery = null;
        MAddCol<CacheResult<ReadSaveInterfaceImplement, IQuery<KeyField, Property>>> hashCaches = getCachedCol(table, implement.getValueComponents().hash, MapCacheAspect.Type.READSAVE);
        synchronized(hashCaches) {
            for(CacheResult<ReadSaveInterfaceImplement, IQuery<KeyField, Property>> cache : hashCaches.it()) {
                for(MapValuesTranslate mapValues : new MapValuesIterable(cache.implement, implement)) {
                    if(cache.implement.translateValues(mapValues).equals(implement)) {
                        cacheQuery = cache.result.translateValues(mapValues.filter(cache.result.getInnerValues())); // так как могут не использоваться values в Query
                        break;
                    }
                }
            }
            if(cacheQuery==null || checkCaches) {
                query = (IQuery<KeyField, Property>) thisJoinPoint.proceed(new Object[]{table, properties, implement.usedChanges});

                assert implement.getContextValues().containsAll(ValueExpr.removeStatic(query.getInnerValues())); // в query не должно быть элементов не из implement.getContextValues

                if(cacheQuery == null)
                    cacheNoBig(implement, hashCaches, query);
                else
                    logCaches(cacheQuery.getQuery(),query,thisJoinPoint,"READSAVEQUERY", null);

                logger.info("readSaveQuery - not cached "+table.getName());
                incrementMissed(CacheType.READ_SAVE);
            } else {
                query = cacheQuery;

                logger.info("readSaveQuery - cached "+table.getName());
                incrementHit(CacheType.READ_SAVE);
            }
        }

        if (checkCaches && cacheQuery!=null && !BaseUtils.hashEquals(query, cacheQuery))
            query = query;
        return query;
    }

    @Around("execution(* lsfusion.server.physics.exec.db.table.ImplementTable.getReadSaveQuery(lsfusion.base.col.interfaces.immutable.ImSet, lsfusion.server.logics.action.session.change.PropertyChanges)) " +
            "&& target(table) && args(properties, propChanges)")
    public Object callGetIncrementChange(ProceedingJoinPoint thisJoinPoint, ImplementTable table, ImSet properties, PropertyChanges propChanges) throws Throwable {
        return getReadSaveQuery(table, properties, propChanges, thisJoinPoint);
    }
}
