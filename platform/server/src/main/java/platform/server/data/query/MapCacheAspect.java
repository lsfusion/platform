package platform.server.data.query;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import platform.base.BaseUtils;
import platform.base.TwinImmutableObject;
import platform.base.col.ListFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.add.MAddCol;
import platform.base.col.lru.LRUCache;
import platform.base.col.lru.MCacheMap;
import platform.server.caches.*;
import platform.server.caches.hash.HashCodeKeys;
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
import platform.server.session.DataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

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
            return AbstractOuterContext.getOuterKeys(exprs.values());
        }

        public ImSet<Value> getValues() {
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

    private <K,V> Join<V> join(Query<K,V> query, ImMap<K, ? extends Expr> joinExprs, MapValuesTranslate joinValues, final MCacheMap<Integer, MAddCol<CacheResult<JoinImplement<K>, Join<V>>>> joinCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {
//        assert BaseUtils.onlyObjects(joinExprs.keySet()); он вообщем то не нужен, так как hashCaches хранится для Query, а он уже хранит K
        assert ((QueryCacheAspect.QueryCacheInterface)query).getCacheTwin() == query;

        JoinImplement<K> joinImplement = new JoinImplement<K>(joinExprs,joinValues);

        MAddCol<CacheResult<JoinImplement<K>, Join<V>>> hashCaches;
        synchronized(joinCaches) {
            int hashImplement = joinImplement.getInnerComponents(true).hash;
            hashCaches = joinCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = ListFact.mAddCol();
                joinCaches.exclAdd(hashImplement, hashCaches);
            }
        }
        synchronized(hashCaches) {
            for(CacheResult<JoinImplement<K>, Join<V>> cache : hashCaches.it()) {
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
            ImRevMap<Value, Value> bigValues = AbstractValuesContext.getBigValues(joinImplement.getContextValues());
            if(bigValues == null) {
                Join<V> join = (Join<V>) thisJoinPoint.proceed();
                hashCaches.add(new CacheResult<JoinImplement<K>, Join<V>>(joinImplement, join));
                return join;
            } else { // для предотвращения утечки памяти, bigvalues - работа с транслированными объектами, а в конце трансляция назад
                JoinImplement<K> cacheImplement = joinImplement.translateInner(new MapValuesTranslator(bigValues));

                Join<V> join = (Join<V>) thisJoinPoint.proceed(new Object[]{query, cacheImplement.exprs, cacheImplement.mapValues});
                hashCaches.add(new CacheResult<JoinImplement<K>, Join<V>>(cacheImplement, join));

                return new MapJoin<V>((MapTranslate)new MapValuesTranslator(bigValues.reverse()), join);
            }
        }
    }

    @Around("execution(platform.server.data.query.Join platform.server.data.query.Query.joinExprs(platform.base.col.interfaces.immutable.ImMap,platform.server.data.translator.MapValuesTranslate)) && target(query) && args(joinExprs,mapValues)")
    public Object callJoin(ProceedingJoinPoint thisJoinPoint, Query query, ImMap joinExprs, MapValuesTranslate mapValues) throws Throwable {
        return join(query, joinExprs, mapValues, ((QueryCacheAspect.QueryCacheInterface) query).getJoinCache(), thisJoinPoint);
    }

    public interface MapPropertyInterface {
        MCacheMap getExprCache();
        MCacheMap getJoinExprCache();
        MCacheMap getDataChangesCache();
        MCacheMap getIncChangeCache();
        MCacheMap getAutoHintCache();
    }
    public static class MapPropertyInterfaceImplement implements MapPropertyInterface {
        private MCacheMap<Integer,MAddCol<CacheResult<JoinExprInterfaceImplement,Query>>> exprCache;
        public MCacheMap getExprCache() {
            if(exprCache==null)
                exprCache = LRUCache.mSmall(LRUCache.EXP_RARE);
            return exprCache;
        }

        private MCacheMap<Integer,MAddCol<CacheResult<QueryInterfaceImplement,Query>>> joinExprCache;
        public MCacheMap getJoinExprCache() {
            if(joinExprCache==null)
                joinExprCache = LRUCache.mSmall(LRUCache.EXP_RARE);
            return joinExprCache;
        }

        private MCacheMap<Integer,MAddCol<CacheResult<DataChangesInterfaceImplement,DataChangesResult>>> dataChangesCache;
        public MCacheMap getDataChangesCache() {
            if(dataChangesCache==null)
                dataChangesCache = LRUCache.mSmall(LRUCache.EXP_RARE);
            return dataChangesCache;
        }

        private MCacheMap<Integer,MAddCol<CacheResult<PropertyChanges, PropertyChange>>> incChangeCache;
        public MCacheMap getIncChangeCache() {
            if(incChangeCache==null)
                incChangeCache = LRUCache.mSmall(LRUCache.EXP_RARE);
            return incChangeCache;
        }

        private MCacheMap<Integer,MAddCol<CacheResult<PropertyChanges, PropertyChange>>> autoHintCache;
        public MCacheMap getAutoHintCache() {
            if(autoHintCache==null)
                autoHintCache = LRUCache.mSmall(LRUCache.EXP_RARE);
            return autoHintCache;
        }
    }
    @DeclareParents(value="platform.server.logics.property.CalcProperty",defaultImpl= MapPropertyInterfaceImplement.class)
    private MapPropertyInterface mapPropertyInterface;

    public static boolean checkCaches = false;

    public <K extends PropertyInterface> ImSet<CalcProperty> getUsedChanges(CalcProperty<K> property, StructChanges implement, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        if(!(property instanceof FunctionProperty) && !(property instanceof DataProperty && ((DataProperty) property).event!=null)) // если не Function или DataProperty с derived, то нету рекурсии и эффективнее просто вы
            return (ImSet<CalcProperty>) thisJoinPoint.proceed();

        // оптимизация самого верхнего уровня, проверка на isEmpty нужна для того чтобы до finalize'a (в CaseUnionProperty в частности) не вызвать случайно getRecDepends, там в fillDepends на это assert есть
        return (ImSet<CalcProperty>) CacheAspect.lazyIdentityExecute(property, thisJoinPoint, new Object[]{!implement.isEmpty() ? implement.filter(property.getRecDepends()) : implement}, true, false);
    }

    @Around("execution(* platform.server.logics.property.CalcProperty.getUsedChanges(platform.server.session.StructChanges)) " +
            "&& target(property) && args(changes)")
    public Object callGetUsedChanges(ProceedingJoinPoint thisJoinPoint, CalcProperty property, StructChanges changes) throws Throwable {
        return getUsedChanges(property, changes, thisJoinPoint);
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

        public boolean twins(TwinImmutableObject o) {
            return changes.equals(((DataChangesResult<P>)o).changes) && BaseUtils.nullEquals(where,((DataChangesResult<P>)o).where);
        }

        public int immutableHashCode() {
            return changes.hashCode() * 31 + BaseUtils.nullHash(where);
        }
    }

    public <K extends PropertyInterface> DataChanges getDataChanges(CalcProperty<K> property, PropertyChange<K> change, WhereBuilder changedWheres, PropertyChanges propChanges, MCacheMap<Integer,MAddCol<CacheResult<DataChangesInterfaceImplement,DataChangesResult>>> dataChangesCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        DataChangesInterfaceImplement<K> implement = new DataChangesInterfaceImplement<K>(property,change,propChanges,changedWheres!=null);

        MAddCol<CacheResult<DataChangesInterfaceImplement, DataChangesResult>> hashCaches;
        synchronized(dataChangesCaches) {
            int hashImplement = implement.getInnerComponents(true).hash;
            hashCaches = dataChangesCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = ListFact.mAddCol();
                dataChangesCaches.exclAdd(hashImplement, hashCaches);
            }
        }

        synchronized(hashCaches) {
            for(CacheResult<DataChangesInterfaceImplement, DataChangesResult> cache : hashCaches.it()) {
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

    public static <I extends ValuesContext<I>, R extends TranslateValues<R>> void cacheNoBig(I implement, MAddCol<CacheResult<I, R>> hashCaches, R result) {
        ImRevMap<Value, Value> bigValues = AbstractValuesContext.getBigValues(implement.getContextValues());
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
        public final PropertyChanges usedChanges;
        private final PropertyQueryType changed; // нужно ли условие на изменение, по сути для этого св-ва и делается класс
        private final ImMap<K, ? extends Expr> values;
        private final boolean propClasses;

        QueryInterfaceImplement(CalcProperty<?> property, boolean propClasses, PropertyChanges changes, PropertyQueryType changed, ImMap<K, ? extends Expr> values) {
            usedChanges = property.getUsedChanges(changes);
            this.propClasses = propClasses;
            this.changed = (changed == PropertyQueryType.RECURSIVE ? PropertyQueryType.CHANGED : changed);
            this.values = values;
        }

        protected ImSet<KeyExpr> getKeys() {
            return SetFact.EMPTY();
        }

        @Override
        public boolean twins(TwinImmutableObject o) {
            return changed == ((QueryInterfaceImplement<K>)o).changed && propClasses == ((QueryInterfaceImplement<K>)o).propClasses && usedChanges.equals(((QueryInterfaceImplement<K>)o).usedChanges) && values.equals(((QueryInterfaceImplement<K>)o).values);
        }

        protected boolean isComplex() {
            return true;
        }
        protected int hash(HashValues hashValues) {
            int hash = 0;
            for(int i=0,size=values.size();i<size;i++)
                hash += values.getKey(i).hashCode() ^ values.getValue(i).hashOuter(HashContext.create(HashCodeKeys.instance, hashValues));
            return 31 * (31 * (usedChanges.hashValues(hashValues) * 31 + hash) + changed.hashCode()) + (propClasses?1:0);
        }

        public ImSet<Value> getValues() {
            return AbstractOuterContext.getOuterValues(values.values()).merge(usedChanges.getContextValues());
        }

        QueryInterfaceImplement(QueryInterfaceImplement<K> implement, MapValuesTranslate translator) {
            usedChanges = implement.usedChanges.translateValues(translator);
            this.changed = implement.changed;
            this.propClasses = implement.propClasses;
            this.values = translator.mapKeys().translate(implement.values);  // assert что keys'ов нет
        }

        protected QueryInterfaceImplement<K> translate(MapValuesTranslate translator) {
            return new QueryInterfaceImplement<K>(this, translator);
        }
    }

    public <K extends PropertyInterface> IQuery<K, String> getQuery(CalcProperty<K> property, boolean propClasses, PropertyChanges propChanges, PropertyQueryType queryType, ImMap<K, ? extends Expr> interfaceValues, MCacheMap<Integer, MAddCol<CacheResult<QueryInterfaceImplement<K>, ValuesContext>>> exprCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {
        // assert что в interfaceValues только values

        if(disableCaches)
            return (IQuery<K, String>) thisJoinPoint.proceed();

        property.cached = true;

        QueryInterfaceImplement<K> implement = new QueryInterfaceImplement<K>(property,propClasses,propChanges,queryType,interfaceValues);

        MAddCol<CacheResult<QueryInterfaceImplement<K>, ValuesContext>> hashCaches;
        synchronized(exprCaches) {
            int hashImplement = implement.getValueComponents().hash;
            hashCaches = exprCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = ListFact.mAddCol();
                exprCaches.exclAdd(hashImplement, hashCaches);
            }
        }

        ValuesContext query = null;
        ValuesContext cacheQuery = null;
        synchronized(hashCaches) {
            for(CacheResult<QueryInterfaceImplement<K>, ValuesContext> cache : hashCaches.it()) {
                for(MapValuesTranslate mapValues : new MapValuesIterable(cache.implement, implement)) {
                    if(cache.implement.translateValues(mapValues).equals(implement)) {
                        ValuesContext<?> cacheResult = (ValuesContext<?>) cache.result;
                        cacheQuery = cacheResult.translateValues(mapValues.filter(cacheResult.getContextValues())); // так как могут не использоваться values в Query
                        break;
                    }
                }
            }
            if(cacheQuery==null || checkCaches) {
                query = (IQuery<K, String>) thisJoinPoint.proceed(new Object[] {property, propClasses, implement.usedChanges, queryType, interfaceValues} );

                assert implement.getContextValues().containsAll(ValueExpr.removeStatic(query.getContextValues())); // в query не должно быть элементов не из implement.getContextValues

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
    @Around("execution(* platform.server.logics.property.CalcProperty.getQuery(boolean,platform.server.session.PropertyChanges,platform.server.logics.property.PropertyQueryType,platform.base.col.interfaces.immutable.ImMap)) " +
            "&& target(property) && args(propClasses,propChanges,queryType,interfaceValues)")
    public Object callGetQuery(ProceedingJoinPoint thisJoinPoint, CalcProperty property, boolean propClasses, PropertyChanges propChanges, PropertyQueryType queryType, ImMap interfaceValues) throws Throwable {
        // сначала target в аспекте должен быть
        return getQuery(property, propClasses, propChanges, queryType, interfaceValues, ((MapPropertyInterface) property).getJoinExprCache(), thisJoinPoint);
    }

    public static class JoinExprInterfaceImplement<P extends PropertyInterface> extends AbstractInnerContext<JoinExprInterfaceImplement<P>> {
        public final PropertyChanges usedChanges;
        private final ImMap<P, Expr> joinImplement;
        private final boolean where;
        private final boolean propClasses;

        public JoinExprInterfaceImplement(CalcProperty<P> property, ImMap<P, Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, boolean where) {
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

        public ImSet<ParamExpr> getKeys() {
            return AbstractOuterContext.getOuterKeys(joinImplement.values());
        }

        public ImSet<Value> getValues() {
            return AbstractOuterContext.getOuterValues(joinImplement.values()).merge(usedChanges.getContextValues());
        }

        private JoinExprInterfaceImplement(JoinExprInterfaceImplement<P> implement, MapTranslate translator) {
            usedChanges = implement.usedChanges.translateValues(translator.mapValues());
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

        public boolean twins(TwinImmutableObject o) {
            return expr.equals(((ExprResult)o).expr) && BaseUtils.nullEquals(where, ((ExprResult)o).where);
        }

        public int immutableHashCode() {
            return expr.hashCode() * 31 + BaseUtils.nullHash(where);
        }
    }

    public static boolean disableCaches = false;

    public <K extends PropertyInterface> Expr getJoinExpr(CalcProperty<K> property, ImMap<K, Expr> joinExprs, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWheres, MCacheMap<Integer, MAddCol<CacheResult<JoinExprInterfaceImplement, ExprResult>>> exprCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {

        // здесь по идее на And не надо проверять
        if(disableCaches || property instanceof FormulaProperty)
            return (Expr) thisJoinPoint.proceed();

        property.cached = true;

        JoinExprInterfaceImplement<K> implement = new JoinExprInterfaceImplement<K>(property,joinExprs,propClasses,propChanges,changedWheres!=null);

        MAddCol<CacheResult<JoinExprInterfaceImplement, ExprResult>> hashCaches;
        synchronized(exprCaches) {
            int hashImplement = implement.getInnerComponents(true).hash;
            hashCaches = exprCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = ListFact.mAddCol();
                exprCaches.exclAdd(hashImplement, hashCaches);
            }
        }

        synchronized(hashCaches) {
            Expr cacheResult = null;
            for(CacheResult<JoinExprInterfaceImplement, ExprResult> cache : hashCaches.it()) {
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
            Expr expr = (Expr) thisJoinPoint.proceed(new Object[]{property, joinExprs, propClasses, implement.usedChanges, cacheWheres});

            cacheNoBig(implement, hashCaches, new ExprResult(expr, changedWheres != null ? cacheWheres.toWhere() : null));
            if(checkCaches && !BaseUtils.hashEquals(expr, cacheResult))
                expr = expr;

            // проверим
            if(checkInfinite && property.isFull())
                expr.checkInfiniteKeys();

            if(changedWheres!=null) changedWheres.add(cacheWheres.toWhere());
            return expr;
        }
    }

    public static boolean checkInfinite = false;

    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("execution(* platform.server.logics.property.CalcProperty.getJoinExpr(platform.base.col.interfaces.immutable.ImMap,boolean,platform.server.session.PropertyChanges,platform.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,propClasses,propChanges,changedWhere)")
    public Object callGetJoinExpr(ProceedingJoinPoint thisJoinPoint, CalcProperty property, ImMap joinExprs, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        // сначала target в аспекте должен быть
        return getJoinExpr(property, joinExprs, propClasses, propChanges, changedWhere, ((MapPropertyInterface) property).getExprCache(), thisJoinPoint);
    }

    public <K extends PropertyInterface> PropertyChange<K> getIncrementChange(CalcProperty<K> property, PropertyChanges propChanges, MCacheMap<Integer, MAddCol<CacheResult<PropertyChanges, PropertyChange<K>>>> exprCaches, ProceedingJoinPoint thisJoinPoint) throws Throwable {
        // assert что в interfaceValues только values

        PropertyChanges implement = property.getUsedChanges(propChanges);

        MAddCol<CacheResult<PropertyChanges, PropertyChange<K>>> hashCaches;
        synchronized(exprCaches) {
            int hashImplement = implement.getValueComponents().hash;
            hashCaches = exprCaches.get(hashImplement);
            if(hashCaches==null) {
                hashCaches = ListFact.mAddCol();
                exprCaches.exclAdd(hashImplement, hashCaches);
            }
        }

        PropertyChange<K> change;
        PropertyChange<K> cacheChange = null;
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

                if(cacheChange== null || !checkCaches)
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

    // aspect нужен по большому счету чтобы сохранять query, и не делать лишних getInnerComponents
    @Around("execution(* platform.server.logics.property.CalcProperty.getIncrementChange(platform.server.session.PropertyChanges)) " +
            "&& target(property) && args(propChanges)")
    public Object callGetIncrementChange(ProceedingJoinPoint thisJoinPoint, CalcProperty property, PropertyChanges propChanges) throws Throwable {
        return getIncrementChange(property, propChanges, ((MapPropertyInterface) property).getIncChangeCache(), thisJoinPoint);
    }
}
