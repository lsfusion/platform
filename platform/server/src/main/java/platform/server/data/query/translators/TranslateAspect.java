package platform.server.data.query.translators;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.classes.where.MeanClassWheres;
import platform.server.data.query.exprs.GroupExpr;
import platform.server.data.query.exprs.MapExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.where.AbstractWhere;
import platform.server.where.Where;

// аспект который заодно транслирует ManualLazy операции
@Aspect
public class TranslateAspect {

    public static interface TranslateLazyInterface {
        void initTranslate(Object object, KeyTranslator translator, Object thisObject);
        Object lazyResult(ProceedingJoinPoint thisJoinPoint) throws Throwable;
    }
    public abstract static class TranslateLazyImplement implements TranslateLazyInterface {
        protected Object object = null;
        protected KeyTranslator translator = null;
        protected boolean translated = false;

        public void initTranslate(Object object, KeyTranslator translator, Object thisObject) {
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

    // SourceExpr, Where, get/calculateWhere
    public static interface TranslateExprLazyInterface extends TranslateLazyInterface {}
    public static class TranslateExprLazyImplement extends TranslateLazyImplement implements TranslateExprLazyInterface {
        protected Where lazyTranslate(ProceedingJoinPoint thisJoinPoint) throws Throwable {
            Where where = ((SourceExpr) object).getWhere();
            if(object instanceof MapExpr) { // не translate'им чтобы бесконечный цикл разорвать
                Where result = (Where) thisJoinPoint.proceed();
                ((TranslateClassWhereLazyInterface)result).initTranslate(where,translator,result);
                return result;
            } else
                return where.translateDirect(translator);
        }
    }
    @DeclareParents(value="@TranslateExprLazy *",defaultImpl=TranslateExprLazyImplement.class)
    private TranslateExprLazyInterface translateExprLazy;
    @AfterReturning(pointcut="call(platform.server.data.query.exprs.SourceExpr platform.server.data.query.exprs.SourceExpr.translateDirect(platform.server.data.query.translators.KeyTranslator)) && target(expr) && args(translator)",returning="transExpr")
    public void afterExprTranslate(SourceExpr expr, KeyTranslator translator, TranslateExprLazyInterface transExpr) {
        transExpr.initTranslate(expr,translator,transExpr);
    }
    @Around("call(platform.server.where.Where platform.server.data.query.exprs.SourceExpr.calculateWhere()) && target(expr)")
    public Object callCalculateWhere(ProceedingJoinPoint thisJoinPoint, TranslateExprLazyInterface expr) throws Throwable {
        return expr.lazyResult(thisJoinPoint);
    }

    // Where, ClassExprWhere, get/calculateClassWhere
    public static interface TranslateClassWhereLazyInterface extends TranslateLazyInterface {}
    public static class TranslateClassWhereLazyImplement extends TranslateLazyImplement implements TranslateClassWhereLazyInterface {
        protected ClassExprWhere lazyTranslate(ProceedingJoinPoint thisJoinPoint) {
            return ((Where)object).getClassWhere().translate(translator);
        }
    }
    @DeclareParents(value="platform.server.where.DataWhere+",defaultImpl=TranslateClassWhereLazyImplement.class)
    private TranslateClassWhereLazyInterface translateClassWhereLazy;
    @AfterReturning(pointcut="call(platform.server.where.Where platform.server.where.Where.translateDirect(platform.server.data.query.translators.KeyTranslator)) && target(where) && args(translator)",returning="transWhere")
    public void afterDataWhereTranslate(AbstractWhere where,KeyTranslator translator, TranslateClassWhereLazyInterface transWhere) {
        if(!(transWhere instanceof MapExpr.NotNull)) // он уже обработан
           transWhere.initTranslate(where,translator,transWhere);
    }
    @Around("call(platform.server.data.classes.where.ClassExprWhere platform.server.where.AbstractWhere.calculateClassWhere()) && target(where)")
    public Object callCalculateClassWhere(ProceedingJoinPoint thisJoinPoint, TranslateClassWhereLazyInterface where) throws Throwable {
        return where.lazyResult(thisJoinPoint);
    }

    // Where, MeanClassWheres, get/calculateMeanClassWheres
    public static interface TranslateMeanWhereLazyInterface extends TranslateLazyInterface {}
    public static class TranslateMeanWhereLazyImplement extends TranslateLazyImplement implements TranslateMeanWhereLazyInterface {
        protected MeanClassWheres lazyTranslate(ProceedingJoinPoint thisJoinPoint) {
            return ((Where)object).getMeanClassWheres().translate(translator);
        }
    }
    @DeclareParents(value="platform.server.where.FormulaWhere+",defaultImpl=TranslateMeanWhereLazyImplement.class)
    private TranslateMeanWhereLazyInterface translateMeanWhereLazy;
    @AfterReturning(pointcut="call(platform.server.where.Where platform.server.where.Where.translateDirect(platform.server.data.query.translators.KeyTranslator)) && target(where) && args(translator)",returning="transWhere")
    public void afterFormulaWhereTranslate(AbstractWhere where,KeyTranslator translator, TranslateMeanWhereLazyInterface transWhere) {
        transWhere.initTranslate(where,translator,transWhere);
    }
    @Around("call(platform.server.data.classes.where.MeanClassWheres platform.server.where.AbstractWhere.calculateMeanClassWheres()) && target(where)")
    public Object callCalculateMeanClassWheres(ProceedingJoinPoint thisJoinPoint, TranslateMeanWhereLazyInterface where) throws Throwable {
        return where.lazyResult(thisJoinPoint);
    }

    // MeanClassWheres, ClassExprWhere, get/calculateWhere
    public static interface TranslateMeanClassWhereLazyInterface extends TranslateLazyInterface {}
    public static class TranslateMeanClassWhereLazyImplement extends TranslateLazyImplement implements TranslateMeanClassWhereLazyInterface {
        protected ClassExprWhere lazyTranslate(ProceedingJoinPoint thisJoinPoint) {
            return ((MeanClassWheres)object).getClassWhere().translate(translator);
        }
    }
    @DeclareParents(value="platform.server.data.classes.where.MeanClassWheres+",defaultImpl=TranslateMeanClassWhereLazyImplement.class)
    private TranslateMeanClassWhereLazyInterface translateMeanClassWhereLazy;
    @AfterReturning(pointcut="call(platform.server.data.classes.where.MeanClassWheres platform.server.data.classes.where.MeanClassWheres.translate(platform.server.data.query.translators.KeyTranslator)) && target(where) && args(translator)",returning="transWhere")
    public void afterMeanClassWhereTranslate(MeanClassWheres where,KeyTranslator translator, TranslateMeanClassWhereLazyInterface transWhere) {
        transWhere.initTranslate(where,translator,transWhere);
    }
    @Around("call(platform.server.data.classes.where.ClassExprWhere platform.server.data.classes.where.MeanClassWheres.calculateClassWhere()) && target(where)")
    public Object callCalculateMeanClassWhere(ProceedingJoinPoint thisJoinPoint, TranslateMeanClassWhereLazyInterface where) throws Throwable {
        return where.lazyResult(thisJoinPoint);
    }

    // packFollowFalse noPush
    @Around("call(platform.server.data.query.exprs.AndExpr platform.server.data.query.exprs.AndExpr.packFollowFalse(platform.server.where.Where)) && target(groupExpr) && args(falseWhere)")
    public Object callPackFollowFalse(ProceedingJoinPoint thisJoinPoint, GroupExpr groupExpr, AbstractWhere falseWhere) throws Throwable {
        if(groupExpr.assertNoPush(falseWhere.not()))
            return groupExpr;
        else
            return thisJoinPoint.proceed();
   }
    /*
    @Around("execution(@platform.server.data.query.translators.KeepObject * *.*(..)) && target(object)") // с call'ом есть баги
    public Object callKeepObject(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Object result = thisJoinPoint.proceed();
        if(object.equals(result)) // сохраним ссылку
            return object;
        else
            return result;
    } */

    @AfterReturning(pointcut="call(* platform.server.where.AbstractWhere.calculateNot()) && target(where)",returning="notWhere")
    public void afterDataWhereTranslate(AbstractWhere where, AbstractWhere notWhere) {
        notWhere.not = where; 
    }

}