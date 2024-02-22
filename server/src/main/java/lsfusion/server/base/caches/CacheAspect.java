package lsfusion.server.base.caches;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.heavy.SoftHashMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSASVSMap;
import lsfusion.base.comb.map.GlobalObject;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
public class CacheAspect {

/*    public static interface ImmutableInterface {
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
  */

    public static class Invocation extends TwinImmutableObject {
        final Method method;
        public final Object[] args;

        Invocation(ProceedingJoinPoint thisJoinPoint, Object[] args) {
            this.method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
            this.args = args;
        }

        @Override
        public String toString() {
            return method +"(" + Arrays.asList(args) + ')';
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return method.equals(((Invocation)o).method) && Arrays.equals(args,((Invocation)o).args);
        }

        public int immutableHashCode() {
            return 31* method.hashCode() + Arrays.hashCode(args);
        }
    }

    private static Object lazyExecute(Object object, ProceedingJoinPoint thisJoinPoint, Object[] args, LRUWSASVSMap<Object, Method, Object, Object> lruCache, boolean changedArgs, CacheStats.CacheType type) throws Throwable {
        Method method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
        Object result = lruCache.get(object, method, args);

        Object checkResult = null;
        if(checkCaches && type != CacheStats.CacheType.INSTANCE_LAZY && type != CacheStats.CacheType.PARAM_INSTANCE_LAZY) {
            checkResult = result;
            result = null;
        }
                    
        if (result == null) {
            CacheStats.incrementMissed(type);
            result = execute(object, thisJoinPoint, args, changedArgs);
            lruCache.put(object, method, args, result == null ? LRUUtil.Value.NULL : result);                        
        } else {
            if (result == LRUUtil.Value.NULL) {
                result = null;
            }
        }
        CacheStats.incrementHit(type);
        
        if(checkCaches && checkResult != null) {
            lruCache.put(object, method, args, checkResult);
            if (checkResult == LRUUtil.Value.NULL) {
                checkResult = null;
            }
            if(!BaseUtils.nullHashEquals(result, checkResult))
                System.out.println("WRONG CACHE : object - " + object + ", method - " + method + ", args - " + Arrays.toString(args) + "\n\tACTUAL RESULT :" + result + "\n\tCACHED RESULT :" + checkResult);
        }
        
        return result;
    }
    
    public static boolean checkNoCachesBoolean(Object object, Type type, Class clazz, String methodName) {
        try {
            Method method = clazz.getMethod(methodName, boolean.class);
            return CacheAspect.checkNoCaches(object, CacheAspect.Type.SIMPLE, method, new Object[]{true})
                    && CacheAspect.checkNoCaches(object, CacheAspect.Type.SIMPLE, method, new Object[]{false});
        } catch (NoSuchMethodException e) {
            throw Throwables.propagate(e);
        }
    }
    public static boolean checkNoCaches(Object object, Type type, Class clazz, String methodName) {
        try {
            Method method = clazz.getDeclaredMethod(methodName);
            return CacheAspect.checkNoCaches(object, type, method, new Object[]{});
        } catch (NoSuchMethodException e) {
            throw Throwables.propagate(e);
        }
    }
    public static boolean checkNoCaches(Object object, Type type, Method method, Object[] args) {
        return getLRUCache(type).get(object, method, args) == null;
    }

    static class IdentityInvocation {
        final WeakReference<Object> targetRef;

        final Method method;
        final Object[] args;

        public IdentityInvocation(ReferenceQueue<Object> refQueue, Object target, ProceedingJoinPoint thisJoinPoint, Object[] args) {
            this.targetRef = new IdentityInvocationWeakMap.InvocationWeakReference(target, refQueue, this);
            this.method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
            this.args = args;
        }

        @Override
        public String toString() {
            return method + "(" + Arrays.asList(args) + ')';
        }

        @Override
        public boolean equals(Object o) { // не проверяем на вхождение и класс потому как повторятся не могут
            IdentityInvocation invocation = (IdentityInvocation) o;
            if (invocation == null) return false;
            Object thisTarget = targetRef.get();
            Object otherTarget = invocation.targetRef.get();
            return thisTarget != null && thisTarget == otherTarget && method.equals(invocation.method) && Arrays.equals(args, invocation.args);
        }

        @Override
        public int hashCode() {
            Object thisTarget = targetRef.get();
            if (thisTarget != null) {
                return 31 * (31 * System.identityHashCode(thisTarget) + method.hashCode()) + Arrays.hashCode(args);
            } else {
                return 31 * method.hashCode() + Arrays.hashCode(args);
            }
        }
    }

    static class IdentityInvocationWeakMap {
        private ReferenceQueue<Object> refQueue = new ReferenceQueue<>();

        private Map<IdentityInvocation, Object> map = new HashMap<>();

        public Object get(IdentityInvocation key) {
            expunge();
            return map.get(key);
        }

        public boolean containsKey(IdentityInvocation key) {
            expunge();
            return map.containsKey(key);
        }

        public Object put(IdentityInvocation key, Object value) {
            expunge();
            return map.put(key, value);
        }

        public Object remove(IdentityInvocation key) {
            expunge();
            return map.remove(key);
        }

        public int size() {
            expunge();
            return map.size();
        }

        public ReferenceQueue<Object> getRefQueue() {
            return refQueue;
        }

        private void expunge() {
            InvocationWeakReference ref;
            while ((ref = (InvocationWeakReference) refQueue.poll()) != null) {
                map.remove(ref.invocation);
                ref.invocation = null;
            }
        }

        public static class InvocationWeakReference extends WeakReference {
            IdentityInvocation invocation;
            public InvocationWeakReference(Object target, ReferenceQueue<Object> refQueue, IdentityInvocation invocation) {
                super(target, refQueue);
                this.invocation = invocation;
            }
        }
    }

//    public final static SoftHashMap<IdentityInvocation, Object> lazyIdentityExecute = new SoftHashMap<IdentityInvocation, Object>();
    private static class ConcurrentIdentityInvocationWeakMap {
        private final int segmentShift;
        private final int segmentMask;
        private final IdentityInvocationWeakMap[] segments;

        public ConcurrentIdentityInvocationWeakMap() {

            final int DEFAULT_CONCURRENCY_LEVEL = LRUUtil.DEFAULT_CONCURRENCY_LEVEL;
            int sshift = 0;
            int ssize = 1;
            while (ssize < DEFAULT_CONCURRENCY_LEVEL) {
                ++sshift;
                ssize <<= 1;
            }
            segmentShift = 32 - sshift;
            segmentMask = ssize - 1;
            this.segments = new IdentityInvocationWeakMap[DEFAULT_CONCURRENCY_LEVEL];

            for (int i = 0; i < segments.length; i++)
                segments[i] = new IdentityInvocationWeakMap();
        }

        private IdentityInvocationWeakMap segmentFor(int hash) {
            return segments[(hash >>> segmentShift) & segmentMask];
        }

        public IdentityInvocationWeakMap segmentFor(Object target) {
            return segmentFor(System.identityHashCode(target));
        }
    }

    public final static ConcurrentIdentityInvocationWeakMap concurrentLazyIdentityExecute = new ConcurrentIdentityInvocationWeakMap();
    public final static IdentityInvocationWeakMap lazyIdentityExecute = new IdentityInvocationWeakMap();
    public final static LRUWSASVSMap<Object, Method, Object, Object> commonLruCache = new LRUWSASVSMap<>(LRUUtil.G2);
    public final static LRUWSASVSMap<Object, Method, Object, Object> quickLruCache = new LRUWSASVSMap<>(LRUUtil.L1);

    private static Object execute(Object target, ProceedingJoinPoint thisJoinPoint, Object[] args, boolean changedArgs) throws Throwable {
        if(changedArgs) {
            Object[] call = new Object[args.length+1];
            call[0] = target;
            System.arraycopy(args, 0, call, 1, args.length);
            return thisJoinPoint.proceed(call);
        } else
            return thisJoinPoint.proceed();
    }

    public enum Type {
        SIMPLE,
        START,
        STRONG,
        STRONG_NOTNULL, // the mode where null is not cached (is used to "cancel" the call)
        QUICK
    } 

    private static class Waiting {}

    private static boolean disableCaches = false;
    private static boolean disableStrongCaches = false;
    private static boolean checkCaches = false;

    public static Object lazyIdentityExecute(Object target, ProceedingJoinPoint thisJoinPoint, Object[] args, boolean changedArgs, Type type, CacheStats.CacheType cacheType) throws Throwable {
        if(type == Type.STRONG || type == Type.STRONG_NOTNULL) {
            if(disableStrongCaches)
                return execute(target, thisJoinPoint, args, changedArgs);

//            synchronized (lazyIdentityExecute) {
//                IdentityInvocation invocation = new IdentityInvocation(lazyIdentityExecute.getRefQueue(), target, thisJoinPoint, args);
//                Object result = lazyIdentityExecute.get(invocation);
//                if (result == null && !lazyIdentityExecute.containsKey(invocation)) { // здесь и в lazyExecute кривовато, но пока такой способ handl'ить null
//                    result = execute(target, thisJoinPoint, args, changedArgs);
//                    lazyIdentityExecute.put(invocation, result);
//                }
//                return result;
//            }

            final IdentityInvocationWeakMap lazyIdentityExecute = concurrentLazyIdentityExecute.segmentFor(target);
            Object result;
            IdentityInvocation invocation;

            synchronized (lazyIdentityExecute) {
                invocation = new IdentityInvocation(lazyIdentityExecute.getRefQueue(), target, thisJoinPoint, args);
                result = lazyIdentityExecute.get(invocation);
                if (result == null && (type == Type.STRONG_NOTNULL || !lazyIdentityExecute.containsKey(invocation))) { // здесь и в lazyExecute кривовато, но пока такой способ handl'ить null
                    result = new Waiting();
                    lazyIdentityExecute.put(invocation, result);
                }
            }

            if(result instanceof Waiting) {
                synchronized (result) { // dead lock по идее не возможен, так как не подразумевает рекурсивный вызов
                    synchronized (lazyIdentityExecute) { // double check
                        final Object doubleResult = lazyIdentityExecute.get(invocation);
                        if(!(doubleResult instanceof Waiting)) // никто не успел высчитать результат до блокировки result
                            return doubleResult;
                    }
                    result = execute(target, thisJoinPoint, args, changedArgs);
                    if(!(result == null && type == Type.STRONG_NOTNULL)) {
                        synchronized (lazyIdentityExecute) {
                            lazyIdentityExecute.put(invocation, result);
                        }
                    }
                }
            }
            return result;
        }

        if(disableCaches)
            return execute(target, thisJoinPoint, args, changedArgs);

        return lazyExecute(target, thisJoinPoint, args, getLRUCache(type), changedArgs, cacheType);     
    }

    public static LRUWSASVSMap<Object, Method, Object, Object> getLRUCache(Type type) {
        LRUWSASVSMap<Object, Method, Object, Object> lruCache = null;
        if(type == Type.QUICK) {
            lruCache = quickLruCache;
        } else {
            if (type == Type.START) {
                BusinessLogics businessLogics = ThreadLocalContext.getBusinessLogics();
                if (businessLogics != null) {
                    lruCache = businessLogics.startLruCache;
                } else
                    ServerLoggers.assertLog(false, "");
            }
            if (lruCache == null)
                lruCache = commonLruCache;
        }
        return lruCache;
    }

    public static Object callMethod(Object object, ProceedingJoinPoint thisJoinPoint, Type type, CacheStats.CacheType cacheType) throws Throwable {
        return lazyIdentityExecute(object, thisJoinPoint, thisJoinPoint.getArgs(), false, type, cacheType);
    }
    @Around("execution(@lsfusion.server.base.caches.IdentityLazy * *.*(..)) && target(object)")
    public Object callMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return callMethod(object, thisJoinPoint, Type.SIMPLE, CacheStats.CacheType.IDENTITY_LAZY);
    }
    @Around("execution(@lsfusion.server.base.caches.IdentityStartLazy * *.*(..)) && target(object)")
    public Object callStartMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return callMethod(object, thisJoinPoint, Type.START, CacheStats.CacheType.IDENTITY_LAZY);
    }
    @Around("execution(@lsfusion.server.base.caches.IdentityInstanceLazy * *.*(..)) && target(object)")
    public Object callInstanceMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return callMethod(object, thisJoinPoint, Type.SIMPLE, CacheStats.CacheType.INSTANCE_LAZY); // есть и для мелких объектов, а в этом случае нужна более быстрая синхронизация
    }
    @Around("execution(@lsfusion.server.base.caches.IdentityStrongLazy * *.*(..)) && target(object)")
    public Object callStrongMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return callMethod(object, thisJoinPoint, Type.STRONG, CacheStats.CacheType.INSTANCE_LAZY);
    }
    @Around("execution(@lsfusion.server.base.caches.IdentityStrongNotNullLazy * *.*(..)) && target(object)")
    public Object callStrongNotNullMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return callMethod(object, thisJoinPoint, Type.STRONG_NOTNULL, CacheStats.CacheType.INSTANCE_LAZY);
    }
    @Around("execution(@lsfusion.server.base.caches.IdentityQuickLazy * *.*(..)) && target(object)")
    public Object callQuickMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return callMethod(object, thisJoinPoint, Type.QUICK, CacheStats.CacheType.QUICK_LAZY);
    }

    public static Object callParamMethod(Object object, ProceedingJoinPoint thisJoinPoint, Type type, CacheStats.CacheType cacheType) throws Throwable {
        Object[] args = thisJoinPoint.getArgs();
        Object[] switchArgs = new Object[args.length];
        switchArgs[0] = object;
        System.arraycopy(args, 1, switchArgs, 1, args.length - 1);

        return lazyIdentityExecute(args[0], thisJoinPoint, switchArgs, false, type, cacheType);
    }
    @Around("execution(@lsfusion.server.base.caches.ParamLazy * *.*(..)) && target(object)")
    public Object callParamMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return callParamMethod(object, thisJoinPoint, Type.SIMPLE, CacheStats.CacheType.PARAM_LAZY);
    }

    @Around("execution(@lsfusion.server.base.caches.ParamInstanceLazy * *.*(..)) && target(object)")
    public Object callParamInstanceMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return callParamMethod(object, thisJoinPoint, Type.SIMPLE, CacheStats.CacheType.PARAM_INSTANCE_LAZY);
    }

    //@net.jcip.annotations.Immutable
    @Around("execution(@lsfusion.server.base.caches.SynchronizedLazy * *.*(..)) && target(object)")
    public Object callSynchronizedMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        synchronized (object) {
            return callMethod(thisJoinPoint, object);
        }
    }

    static class TwinInvocation {
        final Object object;
        final Method method;
        final Object[] args;

        TwinInvocation(Object object, ProceedingJoinPoint thisJoinPoint, Object[] args) {
            this.object = object;
            this.method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
            this.args = args;
        }

        @Override
        public String toString() {
            return object + "." + method + "(" + Arrays.asList(args) + ')';
        }

        @Override
        public boolean equals(Object o) { // не проверяем на вхождение и класс потому как повторятся не могут
            return o != null && object.equals(((TwinInvocation) o).object) && method.equals(((TwinInvocation) o).method) && Arrays.equals(args, ((TwinInvocation) o).args);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * object.hashCode() + method.hashCode()) + Arrays.hashCode(args);
        }
    }

    public final static SoftHashMap<TwinInvocation, Object> lazyTwinExecute = new SoftHashMap<>();

    private Object lazyTwinExecute(Object object, ProceedingJoinPoint thisJoinPoint, Object[] args) throws Throwable {
        TwinInvocation invoke = new TwinInvocation(object, thisJoinPoint, args);
        Object result = lazyTwinExecute.get(invoke);
        if (result == null) {
            result = thisJoinPoint.proceed();
            lazyTwinExecute.put(invoke, result);
        }

        return result;
    }

    @Around("execution(@lsfusion.server.base.caches.TwinLazy * *.*(..)) && target(object)")
    // с call'ом есть баги
    public Object callTwinMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return lazyIdentityExecute(object, thisJoinPoint, thisJoinPoint.getArgs(), false, Type.SIMPLE, CacheStats.CacheType.TWIN_LAZY);
//        return lazyTwinExecute(object, thisJoinPoint, thisJoinPoint.getArgs());
    }

    // не synchronized, но не используется
    public final static SoftHashMap<Object, Object> lazyTwinManualExecute = new SoftHashMap<>();
    private Object lazyTwinManualExecute(Object object, ProceedingJoinPoint thisJoinPoint, Object[] args) throws Throwable {
        Object twin = lazyTwinManualExecute.get(object);
        if(twin==null) {
            twin = object;
            lazyTwinManualExecute.put(object, object);
        }
        if (twin == object)
            return thisJoinPoint.proceed();
        else // нужно вызвать тот же метод но twin объекта
            return thisJoinPoint.proceed(BaseUtils.add(new Object[]{twin}, args));
    }
/*    @Around("execution(@TwinManualLazy * *.*(..)) && target(object)")
    public Object callTwinManualMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return lazyTwinManualExecute(object, thisJoinPoint, thisJoinPoint.getArgs());
    }*/
    
    @Around("execution(@lsfusion.server.base.caches.ParamTwinLazy * *.*(..)) && target(object)")
    // с call'ом есть баги
    // не synchronized, но не используется
    public Object callParamTwinMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Object[] args = thisJoinPoint.getArgs();
        Object[] switchArgs = new Object[args.length];
        switchArgs[0] = object;
        System.arraycopy(args, 1, switchArgs, 1, args.length - 1);

        return lazyTwinExecute(args[0], thisJoinPoint, switchArgs);
    }

    public final static ConcurrentHashMap<Object, Object> twins = MapFact.getGlobalConcurrentHashMap();
    public static <T extends GlobalObject> T twinObject(T object) {
        T twin = (T) twins.get(object);
        if(twin!=null)
            return twin;

        twins.put(object, object);
        return object;
    }
}
