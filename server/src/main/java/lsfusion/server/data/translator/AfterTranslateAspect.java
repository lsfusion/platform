package lsfusion.server.data.translator;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.lru.LRUWVWSMap;
import lsfusion.server.caches.AbstractTranslateContext;
import lsfusion.server.caches.CacheAspect;
import lsfusion.server.caches.CacheStats;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.NotNullExpr;
import lsfusion.server.data.query.AbstractSourceJoin;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.SourceJoin;
import lsfusion.server.data.query.innerjoins.GroupStatType;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.where.AbstractWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.MeanClassWheres;
import lsfusion.server.session.PropertyChange;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;


// аспект который заодно транслирует ManualLazy операции
@Aspect
public class AfterTranslateAspect {

    @Around("execution(lsfusion.server.data.where.Where lsfusion.server.data.expr.Expr.calculateWhere()) && target(expr)")
    public Object callCalculateWhere(ProceedingJoinPoint thisJoinPoint, Expr expr) throws Throwable {
//        Expr from = expr.getFrom();
//        MapTranslate translator = expr.getTranslator();
//        if(from!=null && translator!=null) { // объект не ушел
//            Where fromResult = from.getWhere();
        LRUWVWSMap.Value<MapTranslate, Expr> fromPair = expr.getFromValue();
        MapTranslate translator = fromPair.getLRUKey();
        if(translator!=null) {
            Where fromResult = fromPair.getLRUValue().getWhere();
            if(expr instanceof NotNullExpr && ((NotNullExpr) expr).hasNotNull()) { // если результат использует сам объект, то вычисляем а затем в явную проставляем транслятор от основного объекта (если тот был посчитан или всегда)
                AbstractTranslateContext calcObject = (AbstractTranslateContext) thisJoinPoint.proceed();
                calcObject.initTranslate(fromResult, translator);
                return calcObject;
            } else
                return fromResult.translateOuter(translator);
        } else
            return thisJoinPoint.proceed();
    }

    @Around("execution(lsfusion.server.data.where.classes.ClassExprWhere lsfusion.server.data.where.AbstractWhere.calculateClassWhere()) && target(where)")
    public Object callCalculateClassWhere(ProceedingJoinPoint thisJoinPoint, AbstractWhere where) throws Throwable {
//        Where from = where.getFrom();
//        MapTranslate translator = where.getTranslator();
//        if(from!=null && translator!=null)
//            return from.getClassWhere().translateOuter(translator);
        LRUWVWSMap.Value<MapTranslate, Where> fromPair = where.getFromValue();
        MapTranslate translator = fromPair.getLRUKey();
        if(translator!=null)
            return fromPair.getLRUValue().getClassWhere().translateOuter(translator);
        else
            return thisJoinPoint.proceed();
    }

    @Around("execution(lsfusion.server.data.where.classes.ClassExprWhere lsfusion.server.data.where.AbstractWhere.calculateClassWhere()) && target(where)")
    public Object callCalculateMeanClassWheres(ProceedingJoinPoint thisJoinPoint, AbstractWhere where) throws Throwable {
//        Where from = where.getFrom();
//        MapTranslate translator = where.getTranslator();
//        if(from!=null && translator!=null)
//            return from.groupMeanClassWheres(true).translateOuter(translator);
        LRUWVWSMap.Value<MapTranslate, Where> fromPair = where.getFromValue();
        MapTranslate translator = fromPair.getLRUKey();
        if(translator!=null)
            return fromPair.getLRUValue().groupMeanClassWheres(true).translateOuter(translator);
        else
            return thisJoinPoint.proceed();
    }

    @Around("execution(lsfusion.server.data.query.innerjoins.KeyEquals lsfusion.server.data.where.AbstractWhere.calculateKeyEquals()) && target(where)")
    public Object callCalculateKeyEquals(ProceedingJoinPoint thisJoinPoint, AbstractWhere where) throws Throwable {
//        Where from = where.getFrom();
//        MapTranslate translator = where.getTranslator();
//        if(from!=null && translator!=null)
//            return from.getKeyEquals().translateOuter(translator);
        LRUWVWSMap.Value<MapTranslate, Where> fromPair = where.getFromValue();
        MapTranslate translator = fromPair.getLRUKey();
        if(translator!=null)
            return fromPair.getLRUValue().getKeyEquals().translateOuter(translator);
        else
            return thisJoinPoint.proceed();
    }


    @Around("execution(lsfusion.server.data.where.classes.ClassExprWhere lsfusion.server.data.where.classes.MeanClassWheres.calculateClassWhere()) && target(wheres)")
    public Object callMeanCalculateClassWhere(ProceedingJoinPoint thisJoinPoint, MeanClassWheres wheres) throws Throwable {
//        MeanClassWheres.OuterContext from = wheres.getOuter().getFrom();
//        MapTranslate translator = wheres.getOuter().getTranslator();
//        if(from!=null && translator!=null)
//            return from.getThis().getClassWhere().translateOuter(translator);
        LRUWVWSMap.Value<MapTranslate, MeanClassWheres.OuterContext> fromPair = wheres.getOuter().getFromValue();
        MapTranslate translator = fromPair.getLRUKey();
        if(translator!=null)
            return fromPair.getLRUValue().getThis().getClassWhere().translateOuter(translator);
        else
            return thisJoinPoint.proceed();
    }

    @Around("execution(* lsfusion.server.data.where.AbstractWhere.getFullStatKeys(lsfusion.base.col.interfaces.immutable.ImSet)) && target(where) && args(groups)")
    public Object callFullStatKeys(ProceedingJoinPoint thisJoinPoint, AbstractWhere where, ImSet groups) throws Throwable {
//        Where from = where.getFrom();
//        MapTranslate translator = where.getTranslator();
//        if(from!=null && translator!=null)
//            return StatKeys.translateOuter(from.getFullStatKeys(translator.reverseMap().translateDirect(groups)), translator);
        LRUWVWSMap.Value<MapTranslate, Where> fromPair = where.getFromValue();
        MapTranslate translator = fromPair.getLRUKey();
        if(translator!=null)
            return StatKeys.translateOuter(fromPair.getLRUValue().getFullStatKeys(translator.reverseMap().translateDirect(groups)), translator);
        else
            return thisJoinPoint.proceed();
    }

    @Around("execution(* lsfusion.server.session.PropertyChange.getQuery()) && target(change)")
    public Object callGetQuery(ProceedingJoinPoint thisJoinPoint, PropertyChange change) throws Throwable {
        return test(thisJoinPoint, change);
    }

    private Object test(ProceedingJoinPoint thisJoinPoint, PropertyChange change) throws Throwable {
//        PropertyChange from = (PropertyChange) change.getFrom();
//        MapTranslate translator = (MapTranslate) change.getTranslator();
//        if(from!=null && translator!=null) {
//            IQuery<?, ?> query = from.getQuery();
        LRUWVWSMap.Value<MapTranslate, PropertyChange> fromPair = change.getFromValue();
        MapTranslate translator = fromPair.getLRUKey();
        if(translator!=null) {
            IQuery<?, ?> query = fromPair.getLRUValue().getQuery();
            return query.translateInner(translator.filterValues(query.getInnerValues()));
        } else
            return thisJoinPoint.proceed();
    }

    @Around("execution(lsfusion.base.Pair lsfusion.server.data.where.AbstractWhere.getWhereJoins(boolean, lsfusion.base.col.interfaces.immutable.ImSet, lsfusion.base.col.interfaces.immutable.ImOrderSet)) && target(where) && args(tryExclusive,keepStat,orderTop)")
    public Object callGetWhereJoins(ProceedingJoinPoint thisJoinPoint, AbstractWhere where, boolean tryExclusive, ImSet keepStat, ImOrderSet orderTop) throws Throwable {
        if(keepStat.equals(where.getOuterKeys()) && orderTop.isEmpty())
            return CacheAspect.callMethod(where, thisJoinPoint, CacheAspect.Type.SIMPLE, CacheStats.CacheType.OTHER);
        return thisJoinPoint.proceed();
    }

    @Around("execution(lsfusion.base.col.interfaces.immutable.ImCol lsfusion.server.data.where.AbstractWhere.getStatJoins(boolean, lsfusion.base.col.interfaces.immutable.ImSet, lsfusion.server.data.query.innerjoins.GroupStatType, boolean)) && target(where) && args(exclusive,keepStat,type,noWhere)")
    public Object callGetStatJoins(ProceedingJoinPoint thisJoinPoint, AbstractWhere where, boolean exclusive, ImSet keepStat, GroupStatType type, boolean noWhere) throws Throwable {
        if(keepStat.equals(where.getOuterKeys()))
            return CacheAspect.callMethod(where, thisJoinPoint, CacheAspect.Type.SIMPLE, CacheStats.CacheType.OTHER);
        return thisJoinPoint.proceed();
    }

    @Around("execution(* lsfusion.server.data.query.AbstractSourceJoin.translateQuery(lsfusion.server.data.translator.QueryTranslator)) && target(toTranslate) && args(translator)")
    public Object callTranslateQuery(ProceedingJoinPoint thisJoinPoint, AbstractSourceJoin toTranslate, PartialQueryTranslator translator) throws Throwable {
        ImSet<ParamExpr> keys = ((SourceJoin<?>)toTranslate).getOuterKeys();
        if(keys.disjoint(translator.keys.keys()))
            return toTranslate;
        else
            return thisJoinPoint.proceed();
    }

/*    @Around("execution(lsfusion.server.data.where.classes.ClassExprWhere lsfusion.server.data.where.classes.MeanClassWheres.calculateClassWhere()) && target(wheres)")
    public Object callCalculateMeanClassWhere(ProceedingJoinPoint thisJoinPoint, MeanClassWheres wheres) throws Throwable {
        Object o = wheres.getFrom();
        if(o!=null)
            return where.groupMeanClassWheres().translateOuter(where.getTranslator());
        else
            return thisJoinPoint.proceed();
    }*/
/*    public static interface TranslateLazyInterface {
        void initTranslate(Object object, MapTranslate translator, Object thisObject);
        Object lazyResult(ProceedingJoinPoint thisJoinPoint) throws Throwable;
    }
    public abstract static class TranslateLazyImplement implements TranslateLazyInterface {
        protected Object object = null;
        protected MapTranslate translator = null;
        protected boolean translated = false;

        public void initTranslate(Object object, MapTranslate translator, Object thisObject) {
            if(!translated && !(object==thisObject)) {
                this.object = object;
                this.translator = translator;
            }
        }

        protected abstract Object lazyTranslate(ProceedingJoinPoint thisJoinPoint) throws Throwable;

        public Object lazyResult(ProceedingJoinPoint thisJoinPoint) throws Throwable {
            assert !translated;
            translated = true;
            if(object ==null)
                return thisJoinPoint.proceed();
            else {
                Object result = lazyTranslate(thisJoinPoint);
                object = null;
                translator = null;
                return result;
            }
        }
    }

    // Expr, Where, get/calculateWhere
    public static interface TranslateExprLazyInterface extends TranslateLazyInterface {}
    public static class TranslateExprLazyImplement extends TranslateLazyImplement implements TranslateExprLazyInterface {
        protected Where lazyTranslate(ProceedingJoinPoint thisJoinPoint) throws Throwable {
            Where where = ((Expr) object).getWhere();
            if(object instanceof InnerExpr && !PartitionExpr.isWhereCalculated((Expr)object)) { // не translateOuter'им чтобы бесконечный цикл разорвать
                Where result = (Where) thisJoinPoint.proceed();
                ((TranslateClassWhereLazyInterface)result).initTranslate(where,translator,result);
                return result;
            } else
                return where.translateOuter(translator);
        }
    }
    @DeclareParents(value="@TranslateExprLazy *",defaultImpl=TranslateExprLazyImplement.class)
    private TranslateExprLazyInterface translateExprLazy;
    @AfterReturning(pointcut="call(lsfusion.server.data.expr.Expr lsfusion.server.data.expr.Expr.translateOuter(lsfusion.server.data.translator.MapTranslate)) && target(expr) && args(translator)",returning="transExpr")
    public void afterExprTranslate(Expr expr, MapTranslate translator, TranslateExprLazyInterface transExpr) {
        transExpr.initTranslate(expr,translator,transExpr);
    }
    @Around("call(lsfusion.server.data.where.Where lsfusion.server.data.expr.Expr.calculateWhere()) && target(expr)")
    public Object callCalculateWhere(ProceedingJoinPoint thisJoinPoint, TranslateExprLazyInterface expr) throws Throwable {
        return expr.lazyResult(thisJoinPoint);
    }

    // Where, ClassExprWhere, get/calculateClassWhere
    public static interface TranslateClassWhereLazyInterface extends TranslateLazyInterface {}
    public static class TranslateClassWhereLazyImplement extends TranslateLazyImplement implements TranslateClassWhereLazyInterface {
        protected ClassExprWhere lazyTranslate(ProceedingJoinPoint thisJoinPoint) {
            return ((Where)object).getClassWhere().translateOuter(translator);
        }
    }
    @DeclareParents(value="lsfusion.server.data.where.DataWhere+",defaultImpl=TranslateClassWhereLazyImplement.class)
    private TranslateClassWhereLazyInterface translateClassWhereLazy;

    @AfterReturning(pointcut="call(lsfusion.server.data.where.Where lsfusion.server.data.where.Where.translateOuter(lsfusion.server.data.translator.MapTranslate)) && target(where) && args(translator)",returning="transWhere")
    public void afterDataWhereTranslate(AbstractWhere where, MapTranslate translator, TranslateClassWhereLazyInterface transWhere) {
        if(!(transWhere instanceof InnerExpr.NotNull)) // он уже обработан
           transWhere.initTranslate(where,translator,transWhere);
    }
    @Around("call(lsfusion.server.data.where.classes.ClassExprWhere lsfusion.server.data.where.AbstractWhere.calculateClassWhere()) && target(where)")
    public Object callCalculateClassWhere(ProceedingJoinPoint thisJoinPoint, TranslateClassWhereLazyInterface where) throws Throwable {
        return where.lazyResult(thisJoinPoint);
    }*/
/*
    // Where, MeanClassWheres, get/calculateMeanClassWheres
    public static interface TranslateMeanWhereLazyInterface extends TranslateLazyInterface {}
    public static class TranslateMeanWhereLazyImplement extends TranslateLazyImplement implements TranslateMeanWhereLazyInterface {
        protected MeanClassWheres lazyTranslate(ProceedingJoinPoint thisJoinPoint) {
            return ((Where)object).groupMeanClassWheres().translateOuter(translator);
        }
    }
    @DeclareParents(value="lsfusion.server.data.where.FormulaWhere+",defaultImpl=TranslateMeanWhereLazyImplement.class)
    private TranslateMeanWhereLazyInterface translateMeanWhereLazy;
    @AfterReturning(pointcut="call(lsfusion.server.data.where.Where lsfusion.server.data.where.Where.translateOuter(lsfusion.server.data.translator.MapTranslate)) && target(where) && args(translator)",returning="transWhere")
    public void afterFormulaWhereTranslate(AbstractWhere where, MapTranslate translator, TranslateMeanWhereLazyInterface transWhere) {
        transWhere.initTranslate(where,translator,transWhere);
    }
    @Around("call(lsfusion.server.data.where.classes.MeanClassWheres lsfusion.server.data.where.AbstractWhere.calculateMeanClassWheres()) && target(where)")
    public Object callCalculateMeanClassWheres(ProceedingJoinPoint thisJoinPoint, TranslateMeanWhereLazyInterface where) throws Throwable {
        return where.lazyResult(thisJoinPoint);
    } */
/*
    // MeanClassWheres, ClassExprWhere, get/calculateWhere
    public static interface TranslateMeanClassWhereLazyInterface extends TranslateLazyInterface {}
    public static class TranslateMeanClassWhereLazyImplement extends TranslateLazyImplement implements TranslateMeanClassWhereLazyInterface {
        protected ClassExprWhere lazyTranslate(ProceedingJoinPoint thisJoinPoint) {
            return ((MeanClassWheres)object).getClassWhere().translateOuter(translator);
        }
    }
    @DeclareParents(value="lsfusion.server.data.where.classes.MeanClassWheres+",defaultImpl=TranslateMeanClassWhereLazyImplement.class)
    private TranslateMeanClassWhereLazyInterface translateMeanClassWhereLazy;
    @AfterReturning(pointcut="call(lsfusion.server.data.where.classes.MeanClassWheres lsfusion.server.data.where.classes.MeanClassWheres.translateOuter(lsfusion.server.data.translator.MapTranslate)) && target(where) && args(translator)",returning="transWhere")
    public void afterMeanClassWhereTranslate(MeanClassWheres where, MapTranslate translator, TranslateMeanClassWhereLazyInterface transWhere) {
        transWhere.initTranslate(where,translator,transWhere);
    }
    @Around("call(lsfusion.server.data.where.classes.ClassExprWhere lsfusion.server.data.where.classes.MeanClassWheres.calculateClassWhere()) && target(where)")
    public Object callCalculateMeanClassWhere(ProceedingJoinPoint thisJoinPoint, TranslateMeanClassWhereLazyInterface where) throws Throwable {
        return where.lazyResult(thisJoinPoint);
    }
*/
    // packFollowFalse noPush
//    @Around("call(lsfusion.server.data.expr.BaseExpr lsfusion.server.data.expr.BaseExpr.packFollowFalse(lsfusion.server.data.where.Where)) && target(groupExpr) && args(falseWhere)")
//    public Object callPackFollowFalse(ProceedingJoinPoint thisJoinPoint, GroupExpr groupExpr, AbstractWhere falseWhere) throws Throwable {
//        if(groupExpr.assertNoPush(falseWhere.not()))
//            return groupExpr;
//        else
//            return thisJoinPoint.proceed();
//   }

//    @Around("execution(@lsfusion.server.table.query.translator.KeepObject * *.*(..)) && target(object)") // с call'ом есть баги
//    public Object callKeepObject(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
//        Object result = thisJoinPoint.proceed();
//        if(object.equals(result)) // сохраним ссылку
//            return object;
//        else
//            return result;
//    }

//    @AfterReturning(pointcut="call(* lsfusion.server.data.where.CheckWhere+.not()) && this(DataWhere) && target(where)",returning="notWhere")
//    public void afterDataWhereTranslate(Where where, NotWhere notWhere) {
//        notWhere.not = where;
//    }
}
