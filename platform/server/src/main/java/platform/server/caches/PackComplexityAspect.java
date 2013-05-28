package platform.server.caches;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import platform.server.Settings;
import platform.server.data.query.IQuery;
import platform.server.data.query.Query;

@Aspect
public class PackComplexityAspect {

//    platform.server.data.query.IQuery
    @Around("execution(@platform.server.caches.Pack * platform.server.data.query.Query.*(..)) && target(query)")
    public Object callPackMethod(ProceedingJoinPoint thisJoinPoint, Query query) throws Throwable {
        IQuery pack = ((Query<?,?>)query).pack();
        if(pack!=query) {
            MethodSignature signature = (MethodSignature) thisJoinPoint.getSignature();
            return pack.getClass().getMethod(signature.getName(), signature.getParameterTypes()).invoke(pack, thisJoinPoint.getArgs());
        }
        return thisJoinPoint.proceed();
    }

    @Around("execution(@platform.server.caches.PackComplex * *.*(..))")
    public Object callPackComplexMethod(ProceedingJoinPoint thisJoinPoint) throws Throwable {
        PackInterface result = (PackInterface) thisJoinPoint.proceed();
        if(Settings.get().getPackOnCacheComplexity() > 0 && result.getComplexity(false) > Settings.get().getPackOnCacheComplexity())
            return result.pack();
        return result;
    }
/*
    @Around("execution(* platform.server.logics.property.Property.getJoinExpr(java.util.Map,platform.server.session.PropertyChanges,platform.server.data.where.WhereBuilder)) " +
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
