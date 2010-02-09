package platform.server.data.translator;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWheres;
import platform.server.data.expr.GroupExpr;
import platform.server.data.expr.MapExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.AbstractWhere;
import platform.server.data.where.Where;

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

    // Expr, Where, get/calculateWhere
    public static interface TranslateExprLazyInterface extends TranslateLazyInterface {}
    public static class TranslateExprLazyImplement extends TranslateLazyImplement implements TranslateExprLazyInterface {
        protected Where lazyTranslate(ProceedingJoinPoint thisJoinPoint) throws Throwable {
            Where where = ((Expr) object).getWhere();
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
    @AfterReturning(pointcut="call(platform.server.data.expr.Expr platform.server.data.expr.Expr.translateDirect(platform.server.data.translator.KeyTranslator)) && target(expr) && args(translator)",returning="transExpr")
    public void afterExprTranslate(Expr expr, KeyTranslator translator, TranslateExprLazyInterface transExpr) {
        transExpr.initTranslate(expr,translator,transExpr);
    }
    @Around("call(platform.server.data.where.Where platform.server.data.expr.Expr.calculateWhere()) && target(expr)")
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
    @DeclareParents(value="platform.server.data.where.DataWhere+",defaultImpl=TranslateClassWhereLazyImplement.class)
    private TranslateClassWhereLazyInterface translateClassWhereLazy;
    @AfterReturning(pointcut="call(platform.server.data.where.Where platform.server.data.where.Where.translateDirect(platform.server.data.translator.KeyTranslator)) && target(where) && args(translator)",returning="transWhere")
    public void afterDataWhereTranslate(AbstractWhere where,KeyTranslator translator, TranslateClassWhereLazyInterface transWhere) {
        if(!(transWhere instanceof MapExpr.NotNull)) // он уже обработан
           transWhere.initTranslate(where,translator,transWhere);
    }
    @Around("call(platform.server.data.where.classes.ClassExprWhere platform.server.data.where.AbstractWhere.calculateClassWhere()) && target(where)")
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
    @DeclareParents(value="platform.server.data.where.FormulaWhere+",defaultImpl=TranslateMeanWhereLazyImplement.class)
    private TranslateMeanWhereLazyInterface translateMeanWhereLazy;
    @AfterReturning(pointcut="call(platform.server.data.where.Where platform.server.data.where.Where.translateDirect(platform.server.data.translator.KeyTranslator)) && target(where) && args(translator)",returning="transWhere")
    public void afterFormulaWhereTranslate(AbstractWhere where,KeyTranslator translator, TranslateMeanWhereLazyInterface transWhere) {
        transWhere.initTranslate(where,translator,transWhere);
    }
    @Around("call(platform.server.data.where.classes.MeanClassWheres platform.server.data.where.AbstractWhere.calculateMeanClassWheres()) && target(where)")
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
    @DeclareParents(value="platform.server.data.where.classes.MeanClassWheres+",defaultImpl=TranslateMeanClassWhereLazyImplement.class)
    private TranslateMeanClassWhereLazyInterface translateMeanClassWhereLazy;
    @AfterReturning(pointcut="call(platform.server.data.where.classes.MeanClassWheres platform.server.data.where.classes.MeanClassWheres.translate(platform.server.data.translator.KeyTranslator)) && target(where) && args(translator)",returning="transWhere")
    public void afterMeanClassWhereTranslate(MeanClassWheres where,KeyTranslator translator, TranslateMeanClassWhereLazyInterface transWhere) {
        transWhere.initTranslate(where,translator,transWhere);
    }
    @Around("call(platform.server.data.where.classes.ClassExprWhere platform.server.data.where.classes.MeanClassWheres.calculateClassWhere()) && target(where)")
    public Object callCalculateMeanClassWhere(ProceedingJoinPoint thisJoinPoint, TranslateMeanClassWhereLazyInterface where) throws Throwable {
        return where.lazyResult(thisJoinPoint);
    }

    // packFollowFalse noPush
    @Around("call(platform.server.data.expr.BaseExpr platform.server.data.expr.BaseExpr.packFollowFalse(platform.server.data.where.Where)) && target(groupExpr) && args(falseWhere)")
    public Object callPackFollowFalse(ProceedingJoinPoint thisJoinPoint, GroupExpr groupExpr, AbstractWhere falseWhere) throws Throwable {
        if(groupExpr.assertNoPush(falseWhere.not()))
            return groupExpr;
        else
            return thisJoinPoint.proceed();
   }
    /*
    @Around("execution(@platform.server.table.query.translator.KeepObject * *.*(..)) && target(object)") // с call'ом есть баги
    public Object callKeepObject(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Object result = thisJoinPoint.proceed();
        if(object.equals(result)) // сохраним ссылку
            return object;
        else
            return result;
    } */

    @AfterReturning(pointcut="call(* platform.server.data.where.AbstractWhere.calculateNot()) && target(where)",returning="notWhere")
    public void afterDataWhereTranslate(AbstractWhere where, AbstractWhere notWhere) {
        notWhere.not = where; 
    }

}