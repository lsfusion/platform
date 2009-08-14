package platform.server.caches;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


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

    static class Invocation {
        final Method method;
        final Object[] args;

        Invocation(ProceedingJoinPoint thisJoinPoint, Object[] args) {
            this.method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
            this.args = args;
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
        Invocation invoke = new Invocation(thisJoinPoint,thisJoinPoint.getArgs());
        
        Map caches = ((ImmutableInterface)object).getCaches();
        Object result = caches.get(invoke);
        if(result==null) {
            result = thisJoinPoint.proceed();
            caches.put(invoke,result);
        }
        return result;
    }

    @Around("execution(@platform.server.caches.ParamLazy * *.*(@net.jcip.annotations.Immutable *)) && target(object)") // с call'ом есть баги
    public Object callParamMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Object[] args = thisJoinPoint.getArgs();
        Object[] switchArgs = new Object[args.length];
        switchArgs[0] = object;
        System.arraycopy(args, 1, switchArgs, 1, args.length-1);
        Invocation invoke = new Invocation(thisJoinPoint,switchArgs);

        Map caches = ((ImmutableInterface)args[0]).getCaches();
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
