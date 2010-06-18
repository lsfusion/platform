package platform.server.caches;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import org.aspectj.lang.reflect.MethodSignature;
import platform.base.SoftHashMap;
import platform.base.WeakIdentityHashMap;
import platform.base.WeakIdentityHashSet;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.query.AbstractSourceJoin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
        public boolean equals(Object o) { // не проверяем на вхождение и класс потому как повторятся не могут
            return method.equals(((Invocation)o).method) && Arrays.equals(args,((Invocation)o).args);
        }

        @Override
        public int hashCode() {
            return 31* method.hashCode() + Arrays.hashCode(args);
        }
    }

    private Object lazyExecute(Object object,ProceedingJoinPoint thisJoinPoint,Object[] args) throws Throwable {
        if(args.length>0 && args[0] instanceof NoCacheInterface)
            return thisJoinPoint.proceed();

        Invocation invoke = new Invocation(thisJoinPoint,args);
        Map caches = ((ImmutableInterface)object).getCaches();
        Object result = caches.get(invoke);
        if(result==null) {
            result = thisJoinPoint.proceed();
            caches.put(invoke,result);
        }
        return result;
    }

    //@net.jcip.annotations.Immutable 
    @Around("execution(@platform.server.caches.Lazy * *.*(..)) && target(object)") // с call'ом есть баги
    public Object callMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return lazyExecute(object,thisJoinPoint,thisJoinPoint.getArgs());
    }

    // отдельно для generics из-за бага
    @DeclareParents(value="@platform.server.caches.GenericImmutable *",defaultImpl=ImmutableInterfaceImplement.class)
    private ImmutableInterface genericImmutable;

    //@platform.server.caches.GenericImmutable
    @Around("execution(@platform.server.caches.GenericLazy * *.*(..)) && target(object)") // с call'ом есть баги
    public Object callGenericMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return lazyExecute(object,thisJoinPoint,thisJoinPoint.getArgs());
    }

    //@net.jcip.annotations.Immutable *
    @Around("execution(@platform.server.caches.ParamLazy * *.*(..)) && target(object)") // с call'ом есть баги
    public Object callParamMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Object[] args = thisJoinPoint.getArgs();
        Object[] switchArgs = new Object[args.length];
        switchArgs[0] = object;
        System.arraycopy(args, 1, switchArgs, 1, args.length-1);

        return lazyExecute(args[0],thisJoinPoint,switchArgs);
    }

    //@net.jcip.annotations.Immutable
    @Around("execution(@platform.server.caches.SynchronizedLazy * *.*(..)) && target(object)") // с call'ом есть баги
    public Object callSynchronizedMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        synchronized(object) {
            return callMethod(thisJoinPoint, object);
        }
    }
    
    static class TwinInvocation {
        final Object object;
        final Method method;
        final Object[] args;

        TwinInvocation(Object object,ProceedingJoinPoint thisJoinPoint, Object[] args) {
            this.object = object;
            this.method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
            this.args = args;
        }

        @Override
        public String toString() {
            return object+"."+method +"(" + Arrays.asList(args) + ')';
        }

        @Override
        public boolean equals(Object o) { // не проверяем на вхождение и класс потому как повторятся не могут
            return o!=null && object.equals(((TwinInvocation)o).object) && method.equals(((TwinInvocation)o).method) && Arrays.equals(args,((TwinInvocation)o).args);
        }

        @Override
        public int hashCode() {
            return 31* (31 * object.hashCode() + method.hashCode()) + Arrays.hashCode(args);
        }
    }

    public final static SoftHashMap<TwinInvocation,Object> lazyTwinExecute = new SoftHashMap<TwinInvocation, Object>();

    private Object lazyTwinExecute(Object object,ProceedingJoinPoint thisJoinPoint, Object[] args) throws Throwable {
        TwinInvocation invoke = new TwinInvocation(object,thisJoinPoint,args);
        Object result = lazyTwinExecute.get(invoke);
        if(result==null) {
            result = thisJoinPoint.proceed();
            lazyTwinExecute.put(invoke,result);
        }

        return result;
    }

    @Around("execution(@platform.server.caches.TwinLazy * *.*(..)) && target(object)") // с call'ом есть баги
    public Object callTwinMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return lazyTwinExecute(object, thisJoinPoint, thisJoinPoint.getArgs());
    }

    @Around("execution(@platform.server.caches.ParamTwinLazy * *.*(..)) && target(object)") // с call'ом есть баги
    public Object callParamTwinMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Object[] args = thisJoinPoint.getArgs();
        Object[] switchArgs = new Object[args.length];
        switchArgs[0] = object;
        System.arraycopy(args, 1, switchArgs, 1, args.length-1);

        return lazyTwinExecute(args[0],thisJoinPoint,switchArgs);
    }
    
    public static class TwinsCall {
        Object twin1;
        Object twin2;

        TwinsCall(Object twin1, Object twin2) {
            this.twin1 = twin1;
            this.twin2 = twin2;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof TwinsCall &&
                    ((twin1==((TwinsCall) o).twin1 && twin2==((TwinsCall) o).twin2) || 
                    (twin2==((TwinsCall) o).twin1 && twin1==((TwinsCall) o).twin2));
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(twin1) + System.identityHashCode(twin2);
        }
    }

    static class Twins {
        WeakIdentityHashSet<Object> objects;
        HashSet<Twins> diff;

        Twins(Object object) {
            objects = new WeakIdentityHashSet<Object>(object);
            diff = new HashSet<Twins>();
        }
    }

    static class TwinsMap extends WeakIdentityHashMap<Object, Twins> {

        Twins getTwins(Object object) {
            Twins twins = get(object);
            if(twins ==null) {
                twins = new Twins(object);
                put(object, twins);
            }
            return twins;
        }

        public boolean call(ProceedingJoinPoint thisJoinPoint,Object object1,Object object2) throws Throwable {
            Twins twins1 = getTwins(object1);
            Twins twins2 = getTwins(object2);

            if(twins1.equals(twins2))
                return true;

            if(twins1.diff.contains(twins2))
                return false;

            if((Boolean)thisJoinPoint.proceed()) {
                // "перекидываем" все компоненты в первую
                for(Object object : twins2.objects)
                    put(object, twins1);
                // сливаем компоненты
                twins2.objects.addAll(twins1.objects);
                // сливаем diff'ы
                twins1.diff.addAll(twins2.diff);
                for(Twins eqd : twins2.diff) { // заменяем equal2 на equal1
                    eqd.diff.remove(twins2);
                    eqd.diff.add(twins1);
                }
                return true;
            } else {
                twins1.diff.add(twins2);
                twins2.diff.add(twins1);
                return false;
            }
        }
    }

    public static TwinsMap cacheTwins = new TwinsMap();
    @Around("execution(boolean platform.server.data.query.AbstractSourceJoin.twins(platform.server.data.query.AbstractSourceJoin)) && target(object) && args(twin)") // с call'ом есть баги
    public Object callTwins(ProceedingJoinPoint thisJoinPoint, GroupExpr object, AbstractSourceJoin twin) throws Throwable {
        return cacheTwins.call(thisJoinPoint, object, twin);
    }
}
