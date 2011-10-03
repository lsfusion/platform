package platform.server.data.translator;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import platform.server.caches.CacheAspect;
import platform.server.caches.InnerContext;
import platform.server.caches.MapValues;
import platform.server.caches.hash.HashObject;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.SourceJoin;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

@Aspect
public class TranslateAspect {

    @Around("call(* *.translateOuter(platform.server.data.translator.MapTranslate)) && target(toTranslate)  && args(translator)")
    public Object callTranslateOuter(ProceedingJoinPoint thisJoinPoint, SourceJoin toTranslate, MapTranslate translator) throws Throwable {
        Set<Value> values = new HashSet<Value>();
        toTranslate.enumValues(values);
        if(translator.identityValues(values))
            return toTranslate;
        else
            return thisJoinPoint.proceed();
    }

    @Around("call(* *.translateInner(platform.server.data.translator.MapTranslate)) && target(toTranslate)  && args(translator)")
    public Object callTranslateInner(ProceedingJoinPoint thisJoinPoint, InnerContext toTranslate, MapTranslate translator) throws Throwable {
        if(translator.identityValues(toTranslate.getValues()))
            return toTranslate;
        else
            return thisJoinPoint.proceed();
    }

    @Around("call(* *.translateQuery(platform.server.data.translator.QueryTranslator)) && target(toTranslate)  && args(translator)")
    public Object callTranslateQuery(ProceedingJoinPoint thisJoinPoint, SourceJoin toTranslate, PartialQueryTranslator translator) throws Throwable {
        Set<KeyExpr> keys = new HashSet<KeyExpr>();
        toTranslate.enumKeys(keys);
        if(Collections.disjoint(translator.keys.keySet(),keys))
            return toTranslate;
        else
            return thisJoinPoint.proceed();
    }

    @Around("call(* platform.server.caches.MapValues.translate(platform.server.data.translator.MapValuesTranslate)) && target(toTranslate)  && args(translator)")
    public Object callTranslateMapValues(ProceedingJoinPoint thisJoinPoint, MapValues toTranslate, MapValuesTranslate translator) throws Throwable {
        if(translator.identityValues(toTranslate.getValues()))
            return toTranslate;
        else
            return thisJoinPoint.proceed();
    }

    //@net.jcip.annotations.Immutable
    @Around("execution(@platform.server.data.translator.HashLazy * *.*(..)) && target(object)")
    // с call'ом есть баги
    public Object callMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        HashObject hashObject = (HashObject) thisJoinPoint.getArgs()[0];
        if(hashObject.isGlobal())
            return CacheAspect.callMethod(object, thisJoinPoint); // записываем как IdentityLazy
        else { // тут по сути и параметр должен быть identity, и объект, поэтому пока проще сделать вручную
            IdentityHashMap<Object,Integer> identityCaches = hashObject.getIdentityCaches();
            Integer result = identityCaches.get(object);
            if(result==null) {
                result = (Integer) thisJoinPoint.proceed();
                identityCaches.put(object, result);
            }
            return result;
       }
    }
}
