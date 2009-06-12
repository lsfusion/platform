package platform.server.caches;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import platform.server.data.query.JoinQuery;
import platform.server.data.classes.where.AndClassWhere;
import platform.server.logics.properties.Property;

import java.util.*;
import java.lang.reflect.Method;


@Aspect
public class CacheAspect {

    public static interface ImmutableInterface {
        Map getCaches();
    }
    public static class ImmutableInterfaceImplement implements ImmutableInterface {
        public ImmutableInterfaceImplement() {
        }

        private Map caches = null;
        public Map getCaches() {
            if(caches==null) caches = new HashMap();
            return caches;
        }
    }
    @DeclareParents(value="@net.jcip.annotations.Immutable *",defaultImpl=ImmutableInterfaceImplement.class)
    private ImmutableInterface immutable;

    class Invocation {
        Method method;
        Object[] args;

        Invocation(ProceedingJoinPoint thisJoinPoint) {
            method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
            args = thisJoinPoint.getArgs();
        }

        @Override
        public String toString() {
            return method +"(" + Arrays.asList(args) + ')';
        }

        @Override
        public boolean equals(Object o) {
            return method.equals(((Invocation) o).method) && Arrays.equals(args, ((Invocation) o).args);
        }

        @Override
        public int hashCode() {
            return 31 * method.hashCode() + Arrays.hashCode(args);
        }
    }

    @Around("execution(@platform.server.caches.Lazy * (@net.jcip.annotations.Immutable *).*(..)) && target(object)") // с call'ом есть баги
    public Object callMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Map caches = ((ImmutableInterface)object).getCaches();
        Invocation invoke = new Invocation(thisJoinPoint);
        Object result = caches.get(invoke);
        if(result==null) {
            result = thisJoinPoint.proceed();
            caches.put(invoke,result);
        }
        return result;
    }

    @Around("execution(@platform.server.caches.SynchronizedLazy * (@net.jcip.annotations.Immutable *).*(..)) && target(object)") // с call'ом есть баги
    public Object callSynchronizedMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        synchronized(object) {
            return callMethod(thisJoinPoint, object);
        }
    }

}
