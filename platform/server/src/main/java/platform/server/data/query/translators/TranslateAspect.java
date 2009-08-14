package platform.server.data.query.translators;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.ProceedingJoinPoint;
import platform.server.data.query.JoinData;
import platform.server.data.query.exprs.*;
import platform.server.where.AbstractWhere;
import platform.server.where.OrWhere;
import platform.server.where.DataWhere;
import platform.server.where.AndWhere;

// аспект который заодно транслирует ManualLazy операции
@Aspect
public class TranslateAspect {

    @AfterReturning(pointcut="call(platform.server.data.query.exprs.SourceExpr platform.server.data.query.exprs.SourceExpr.translate(platform.server.data.query.translators.Translator)) && target(expr) && args(translator)",returning="transExpr")
    public void afterExprTranslate(SourceExpr expr, Translator translator,SourceExpr transExpr) {
        if(expr.where!=null && !(expr instanceof JoinData) && !(expr instanceof KeyExpr))
            transExpr.where = expr.where.translate(translator);
    }

    @AfterReturning(pointcut="call(platform.server.where.Where platform.server.where.Where.translate(platform.server.data.query.translators.Translator)) && target(where) && args(translator)",returning="transWhere")
    public void afterWhereTranslate(AbstractWhere where,KeyTranslator translator,AbstractWhere transWhere) {
        if(where.classWhere!=null)
            transWhere.classWhere = where.classWhere.translate(translator);
    }

    @Around("call(platform.server.data.query.exprs.AndExpr platform.server.data.query.exprs.AndExpr.packFollowFalse(platform.server.where.Where)) && target(groupExpr) && args(falseWhere)")
    public Object callPackFollowFalse(ProceedingJoinPoint thisJoinPoint, GroupExpr groupExpr, AbstractWhere falseWhere) throws Throwable {
        if(groupExpr.assertNoPush(falseWhere.not()))
            return groupExpr;
        else
            return thisJoinPoint.proceed();
   }

    @Around("execution(@platform.server.data.query.translators.KeepObject * *.*(..)) && target(object)") // с call'ом есть баги
    public Object callKeepObject(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Object result = thisJoinPoint.proceed();
        if(object.equals(result)) // сохраним ссылку
            return object;
        else
            return result;
    }
}