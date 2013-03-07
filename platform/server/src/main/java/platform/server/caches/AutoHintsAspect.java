package platform.server.caches;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.implementations.abs.AMap;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.add.MAddCol;
import platform.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import platform.base.col.lru.MCacheMap;
import platform.server.Settings;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.IQuery;
import platform.server.data.query.MapCacheAspect;
import platform.server.data.query.QueryException;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyQueryType;
import platform.server.session.Modifier;
import platform.server.session.PropertyChanges;
import platform.server.session.SessionModifier;

import static platform.base.BaseUtils.max;

@Aspect
public class AutoHintsAspect {

    public static ThreadLocal<SessionModifier> catchAutoHint = new ThreadLocal<SessionModifier>();

    public static class AutoHintImplement<P extends PropertyInterface> extends AbstractInnerContext<AutoHintImplement<P>> {
        private final PropertyChanges usedChanges;
        private final ImMap<P, Expr> joinImplement;
        private final ImMap<CalcProperty, Byte> usedHints;

        public AutoHintImplement(CalcProperty<P> property, ImMap<P, Expr> joinImplement, SessionModifier modifier) {
            usedChanges = property.getUsedChanges(modifier.getPropertyChanges());
            this.joinImplement = joinImplement;

            ImSet<CalcProperty> depends = property.getRecDepends();
            ImFilterValueMap<CalcProperty,Byte> mvUsedHints = depends.mapFilterValues();
            for(int i=0,size=depends.size();i<size;i++) {
                CalcProperty dependProperty = depends.get(i);
                
                if(modifier.allowHintIncrement(dependProperty)) {
                    byte result = 0;
                    if(modifier.forceHintIncrement(dependProperty))
                        result |= 1;
                    if(modifier.allowNoUpdate(dependProperty)) {
                        result |= 2;
                        if(modifier.forceNoUpdate(dependProperty))
                            result |= 4;
                    }
                    mvUsedHints.mapValue(i, Byte.valueOf(result));
                }
            }
            usedHints = mvUsedHints.immutableValue();
        }

        public boolean equalsInner(AutoHintImplement<P> o) {
            return BaseUtils.nullHashEquals(joinImplement, o.joinImplement) && BaseUtils.hashEquals(usedChanges,o.usedChanges) && BaseUtils.hashEquals(usedHints, o.usedHints);
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(HashContext hashContext) {
            return 31 * (31 * usedChanges.hashValues(hashContext.values) + (joinImplement==null ? 0 : AbstractOuterContext.hashOuter(joinImplement, hashContext))) + usedHints.hashCode();
        }

        public ImSet<KeyExpr> getKeys() {
            if(joinImplement==null)
                return SetFact.EMPTY();
            return AbstractOuterContext.getOuterKeys(joinImplement.values());
        }

        public ImSet<Value> getValues() {
            ImSet<Value> result = usedChanges.getContextValues();
            if(joinImplement!=null)
                result = AbstractOuterContext.getOuterValues(joinImplement.values()).merge(result);
            return result;
        }

        private AutoHintImplement(AutoHintImplement<P> implement, MapTranslate translator) {
            usedChanges = implement.usedChanges.translateValues(translator.mapValues());
            joinImplement = implement.joinImplement == null ? null : translator.translate(implement.joinImplement);
            this.usedHints = implement.usedHints;
        }

        public AutoHintImplement<P> translate(MapTranslate translator) {
            return new AutoHintImplement<P>(this, translator);
        }
    }

    public <P extends PropertyInterface> Object callAutoHint(ProceedingJoinPoint thisJoinPoint, CalcProperty<P> property, ImMap<P, Expr> joinImplement, MCacheMap<Integer, MAddCol<MapCacheAspect.CacheResult<AutoHintImplement, AutoHintException>>> exprCaches, Modifier modifier) throws Throwable {
        if(!Settings.get().isDisableAutoHints() && modifier instanceof SessionModifier) { // && property.hasChanges(modifier) иначе в рекурсию уходит при changeModifier'е, надо было бы внутрь перенести
            SessionModifier sessionModifier = (SessionModifier) modifier;

/*            AutoHintImplement<P> implement = new AutoHintImplement<P>(property, joinImplement, sessionModifier);
            MAddCol<MapCacheAspect.CacheResult<AutoHintImplement,AutoHintException>> hashCaches;
            synchronized(exprCaches) {
                int hashImplement = implement.getInnerComponents(true).hash;
                hashCaches = exprCaches.get(hashImplement);
                if(hashCaches==null) {
                    hashCaches = ListFact.mAddCol();
                    exprCaches.exclAdd(hashImplement, hashCaches);
                }
            }

            AutoHintException cacheHint = null;
            synchronized(hashCaches) {
                for(MapCacheAspect.CacheResult<AutoHintImplement,AutoHintException> cache : hashCaches.it()) {
                    for(MapValuesTranslate mapValues : new MapValuesIterable(cache.implement, implement)) {
                        if(cache.implement.translateValues(mapValues).equals(implement)) {
                            cacheHint = cache.result;
                            break;
                        }
                    }
                }
                
                boolean checkCaches = MapCacheAspect.checkCaches;
                if(cacheHint==null || checkCaches) {
             */
                    catchAutoHint.set(sessionModifier);
                    Object result;
                    try {
                        result = thisJoinPoint.proceed();
                        assert catchAutoHint.get()==modifier;
                        catchAutoHint.set(null);
//                        assert cacheHint == null; вообще не работает так как могла вычислится с другим modifier'ом и закэшироваться
                        return result; // ничего не пишем в кэш
                    } catch (AutoHintException e) {
                        assert catchAutoHint.get()==modifier;
                        catchAutoHint.set(null);

                        if(e.lowstat)
                            sessionModifier.addHintIncrement(e.property);
                        else
                            sessionModifier.addNoUpdate(e.property);

/*                        if(cacheHint==null)
                            MapCacheAspect.cacheNoBig(implement, hashCaches, e);
                        else
                            assert BaseUtils.hashEquals(cacheHint, e);

                        cacheHint = e;*/
                    }
/*                }

                if(cacheHint.lowstat)
                    sessionModifier.addHintIncrement(cacheHint.property);
                else
                    sessionModifier.addNoUpdate(cacheHint.property);*/
                return ((MethodSignature)thisJoinPoint.getSignature()).getMethod().invoke(thisJoinPoint.getTarget(), thisJoinPoint.getArgs());
//            }
        } else
            return thisJoinPoint.proceed();
    }

    @Around("execution(* platform.server.logics.property.CalcProperty.getExpr(platform.base.col.interfaces.immutable.ImMap, platform.server.session.Modifier)) && target(property) && args(map, modifier)")
    public Object callGetExpr(ProceedingJoinPoint thisJoinPoint, CalcProperty property, ImMap map, Modifier modifier) throws Throwable {
        return callAutoHint(thisJoinPoint, property, map, ((MapCacheAspect.MapPropertyInterface) property).getAutoHintCache(), modifier);
    }
    @Around("execution(* platform.server.logics.property.CalcProperty.getIncrementChange(platform.server.session.Modifier)) && target(property) && args(modifier)")
    public Object callGetIncrementChange(ProceedingJoinPoint thisJoinPoint, CalcProperty property, Modifier modifier) throws Throwable {
        return callAutoHint(thisJoinPoint, property, null, ((MapCacheAspect.MapPropertyInterface) property).getAutoHintCache(), modifier);
    }

    @Around("execution(* platform.server.logics.property.CalcProperty.getQuery(boolean,platform.server.session.PropertyChanges,platform.server.logics.property.PropertyQueryType,*)) && target(property) && args(propClasses, propChanges, queryType, interfaceValues)")
    public Object callGetQuery(ProceedingJoinPoint thisJoinPoint, CalcProperty property, boolean propClasses, PropertyChanges propChanges, PropertyQueryType queryType, AMap interfaceValues) throws Throwable {
        return getQuery(thisJoinPoint, property, propClasses, propChanges, queryType, interfaceValues);
    }

    private static boolean hintHasChanges(Where changed, CalcProperty property, PropertyChanges propChanges) {
        if(changed!=null)
            return !changed.isFalse();
        return property.hasChanges(propChanges);
    }
    private static Object getQuery(ProceedingJoinPoint thisJoinPoint, CalcProperty property, boolean propClasses, PropertyChanges propChanges, PropertyQueryType queryType, AMap interfaceValues) throws Throwable {
        assert property.isFull();

        IQuery<?, String> result = (IQuery) thisJoinPoint.proceed();
        if(queryType == PropertyQueryType.RECURSIVE)
            return result;

        SessionModifier catchHint = catchAutoHint.get();
        if(catchHint != null && catchHint.allowHintIncrement(property)) { // неправильно так как может быть не changed
            Where changed = null;
            if(queryType.needChange())
                changed = result.getExpr("changed").getWhere();

            if(catchHint.forceHintIncrement(property) && hintHasChanges(changed, property, propChanges)) // сразу кидаем exception
                throw new AutoHintException(property, true);
            boolean allowNoUpdate = catchHint.allowNoUpdate(property);
            if(allowNoUpdate && catchHint.forceNoUpdate(property) && hintHasChanges(changed, property, propChanges))
                throw new AutoHintException(property, false);

            if(!(Settings.get().isDisableValueAllHints() && !interfaceValues.isEmpty())) {
                Expr expr = result.getExpr("value");

                long exprComplexity = expr.getComplexity(false);
                long whereComplexity = 0;
                if(changed!=null)
                    whereComplexity = changed.getComplexity(false);
                long complexity = max(exprComplexity, whereComplexity);
                if(complexity > Settings.get().getLimitHintIncrementComplexity() && property.hasChanges(propChanges)) // сложность большая, если нет изменений то ничем не поможешь
                    if(interfaceValues.isEmpty() && queryType == PropertyQueryType.FULLCHANGED) {
                        ImRevMap<?, KeyExpr> mapKeys = result.getMapKeys();
                        Expr prevExpr = property.getExpr(mapKeys);
                        if(whereComplexity > Settings.get().getLimitHintIncrementComplexity() || exprComplexity > prevExpr.getComplexity(false) * Settings.get().getLimitGrowthIncrementComplexity()) {
                            if (changed.getStatKeys(mapKeys.valuesSet()).rows.lessEquals(new Stat(Settings.get().getLimitHintIncrementStat())))
                                throw new AutoHintException(property, true);
                            if(allowNoUpdate && complexity > Settings.get().getLimitHintNoUpdateComplexity()) {
                                System.out.println("AUTO HINT NOUPDATE" + property);
                                throw new AutoHintException(property, false);
                            }
                        }
                    } else // запускаем getQuery уже без interfaceValues, соответственно уже оно если надо (в смысле что статистика будет нормальной) кинет exception
                        property.getQuery(propClasses, propChanges, PropertyQueryType.FULLCHANGED, MapFact.EMPTY());
            }
        }
        return result;
    }


    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("execution(* platform.server.logics.property.CalcProperty.getJoinExpr(platform.base.col.interfaces.immutable.ImMap,boolean,platform.server.session.PropertyChanges,platform.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,propClasses,propChanges,changedWhere)")
    public Object callGetJoinExpr(ProceedingJoinPoint thisJoinPoint, CalcProperty property, boolean propClasses,  ImMap joinExprs, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        // сначала target в аспекте должен быть

        if(!property.isFull())
            return thisJoinPoint.proceed();

        SessionModifier catchHint = catchAutoHint.get();
        if(catchHint == null || !catchHint.allowHintIncrement(property)) // неправильно так как может быть не changed
            return thisJoinPoint.proceed();

        WhereBuilder cascadeWhere = CalcProperty.cascadeWhere(changedWhere);
        Expr result = (Expr) thisJoinPoint.proceed(new Object[]{property, propClasses, joinExprs, propChanges, cascadeWhere});

        long complexity = max(result.getComplexity(false), (changedWhere != null ? cascadeWhere.toWhere().getComplexity(false) : 0));
        if(complexity > Settings.get().getLimitHintIncrementComplexity() && property.hasChanges(propChanges))
            property.getQuery(propClasses, propChanges, PropertyQueryType.FULLCHANGED, MapFact.EMPTY()); // по аналогии с верхним
        
        if(changedWhere!=null) changedWhere.add(cascadeWhere.toWhere());
        return result;
    }

    public static class AutoHintException extends QueryException {

        public final CalcProperty property;
        public final boolean lowstat; 
        public AutoHintException(CalcProperty property, boolean lowstat) {
            this.property = property;
            this.lowstat = lowstat;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || getClass() == o.getClass() && lowstat == ((AutoHintException) o).lowstat && property.equals(((AutoHintException) o).property);

        }

        @Override
        public int hashCode() {
            return 31 * property.hashCode() + (lowstat ? 1 : 0);
        }
    }
}
