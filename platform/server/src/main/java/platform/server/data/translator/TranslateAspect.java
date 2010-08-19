package platform.server.data.translator;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import platform.server.caches.MapValues;
import platform.server.caches.InnerContext;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.SourceJoin;
import platform.server.data.Table;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Aspect
public class TranslateAspect {

    @Around("call(* *.translateOuter(platform.server.data.translator.MapTranslate)) && target(toTranslate)  && args(translator)")
    public Object callTranslateOuter(ProceedingJoinPoint thisJoinPoint, SourceJoin toTranslate, MapTranslate translator) throws Throwable {
        Set<ValueExpr> values = new HashSet<ValueExpr>();
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
}
