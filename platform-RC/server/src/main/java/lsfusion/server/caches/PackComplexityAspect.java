package lsfusion.server.caches;

import lsfusion.base.ReflectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import lsfusion.server.Settings;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.Query;

@Aspect
public class PackComplexityAspect {

//    lsfusion.server.data.query.IQuery
    @Around("execution(@lsfusion.server.caches.Pack * lsfusion.server.data.query.Query.*(..)) && target(query)")
    public Object callPackMethod(ProceedingJoinPoint thisJoinPoint, Query query) throws Throwable {
        IQuery pack = ((Query<?,?>)query).pack();
        if(pack!=query) {
            MethodSignature signature = (MethodSignature) thisJoinPoint.getSignature();
            return ReflectionUtils.invokeTransp(pack.getClass().getMethod(signature.getName(), signature.getParameterTypes()), pack, thisJoinPoint.getArgs());
        }
        return thisJoinPoint.proceed();
    }

    @Around("execution(@lsfusion.server.caches.PackComplex * *.*(..))")
    public Object callPackComplexMethod(ProceedingJoinPoint thisJoinPoint) throws Throwable {
        PackInterface result = (PackInterface) thisJoinPoint.proceed();
        if(Settings.get().getPackOnCacheComplexity() > 0 && result.getComplexity(false) > Settings.get().getPackOnCacheComplexity())
            return result.pack();
        return result;
    }
/*
    @Around("execution(* lsfusion.server.logics.property.Property.getJoinExpr(java.util.Map,lsfusion.server.session.PropertyChanges,lsfusion.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,propChanges,changedWhere)")
    public Object callGetJoinExpr(ProceedingJoinPoint thisJoinPoint, Property property, Map joinExprs, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        if(changedWhere==null)
            return thisJoinPoint.proceed();

        WhereBuilder cascadeWhere = Property.cascadeWhere(changedWhere);
        thisJoinPoint.proceed(new Object[]{property, joinExprs, propChanges, cascadeWhere});
        if(Settings.instance.getPackOnCacheComplexity() > 0 && result.getComplexity(false) > Settings.instance.getPackOnCacheComplexity())
            return result.pack();
        return result;
    }
  */
}
