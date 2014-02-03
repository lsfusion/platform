package lsfusion.server.caches;

import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.col.lru.LRUWSASVSMap;
import lsfusion.base.col.lru.LRUWSSVSMap;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import lsfusion.base.*;
import lsfusion.base.col.SetFact;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.query.AbstractSourceJoin;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

        public boolean twins(TwinImmutableObject o) {
            return method.equals(((Invocation)o).method) && Arrays.equals(args,((Invocation)o).args);
        }

        public int immutableHashCode() {
            return 31* method.hashCode() + Arrays.hashCode(args);
        }
    }

    private static Object lazyExecute(Object object, ProceedingJoinPoint thisJoinPoint, Object[] args, boolean changedArgs) throws Throwable {
        Method method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
        Object result = lruCache.get(object, method, args);
        if (result == null) {
            result = execute(object, thisJoinPoint, args, changedArgs);
            lruCache.put(object, method, args, result == null ? LRUUtil.Value.NULL : result);                        
        }
        if (result == LRUUtil.Value.NULL) { 
            result = null;
        }
        return result;
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
        private ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>();

        private Map<IdentityInvocation, Object> map = new HashMap<IdentityInvocation, Object>();

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
    public final static IdentityInvocationWeakMap lazyIdentityExecute = new IdentityInvocationWeakMap();
    public final static LRUWSASVSMap<Object, Method, Object, Object> lruCache = new LRUWSASVSMap<Object, Method, Object, Object>(LRUUtil.G2);
    
    private static Object execute(Object target, ProceedingJoinPoint thisJoinPoint, Object[] args, boolean changedArgs) throws Throwable {
        if(changedArgs) {
            Object[] call = new Object[args.length+1];
            call[0] = target;
            System.arraycopy(args, 0, call, 1, args.length);
            return thisJoinPoint.proceed(call);
        } else
            return thisJoinPoint.proceed();
    }

    public static Object lazyIdentityExecute(Object target, ProceedingJoinPoint thisJoinPoint, Object[] args, boolean changedArgs, boolean strong) throws Throwable {
        if(args.length>0 && args[0] instanceof NoCacheInterface)
            return execute(target, thisJoinPoint, args, changedArgs);

        if (!strong) {
            return lazyExecute(target, thisJoinPoint, args, changedArgs);     
        }

        synchronized (lazyIdentityExecute) {
            IdentityInvocation invocation = new IdentityInvocation(lazyIdentityExecute.getRefQueue(), target, thisJoinPoint, args);
            Object result = lazyIdentityExecute.get(invocation);
            if (result == null && !lazyIdentityExecute.containsKey(invocation)) { // здесь и в lazyExecute кривовато, но пока такой способ handl'ить null
                result = execute(target, thisJoinPoint, args, changedArgs);
                lazyIdentityExecute.put(invocation, result);
            }
            return result;
        }
    }

    public static Object callMethod(Object object, ProceedingJoinPoint thisJoinPoint, boolean strong) throws Throwable {
        return lazyIdentityExecute(object, thisJoinPoint, thisJoinPoint.getArgs(), false, strong);
    }
    @Around("execution(@lsfusion.server.caches.IdentityLazy * *.*(..)) && target(object)")
    public Object callMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return callMethod(object, thisJoinPoint, false);
    }
    @Around("execution(@lsfusion.server.caches.IdentityInstanceLazy * *.*(..)) && target(object)")
    public Object callInstanceMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return callMethod(object, thisJoinPoint, false); // есть и для мелких объектов, а в этом случае нужна более быстрая синхронизация
    }
    @Around("execution(@lsfusion.server.caches.IdentityStrongLazy * *.*(..)) && target(object)")
    public Object callStrongMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return callMethod(object, thisJoinPoint, true);
    }

    public static Object callParamMethod(Object object, ProceedingJoinPoint thisJoinPoint, boolean strong) throws Throwable {
        Object[] args = thisJoinPoint.getArgs();
        Object[] switchArgs = new Object[args.length];
        switchArgs[0] = object;
        System.arraycopy(args, 1, switchArgs, 1, args.length - 1);

        return lazyIdentityExecute(args[0], thisJoinPoint, switchArgs, false, strong);
    }
    @Around("execution(@lsfusion.server.caches.ParamLazy * *.*(..)) && target(object)")
    public Object callParamMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return callParamMethod(object, thisJoinPoint, false);
    }

    //@net.jcip.annotations.Immutable
    @Around("execution(@lsfusion.server.caches.SynchronizedLazy * *.*(..)) && target(object)")
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

    public final static SoftHashMap<TwinInvocation, Object> lazyTwinExecute = new SoftHashMap<TwinInvocation, Object>();

    private Object lazyTwinExecute(Object object, ProceedingJoinPoint thisJoinPoint, Object[] args) throws Throwable {
        TwinInvocation invoke = new TwinInvocation(object, thisJoinPoint, args);
        Object result = lazyTwinExecute.get(invoke);
        if (result == null) {
            result = thisJoinPoint.proceed();
            lazyTwinExecute.put(invoke, result);
        }

        return result;
    }

    @Around("execution(@lsfusion.server.caches.TwinLazy * *.*(..)) && target(object)")
    // с call'ом есть баги
    public Object callTwinMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return lazyIdentityExecute(object, thisJoinPoint, thisJoinPoint.getArgs(), false, false);
//        return lazyTwinExecute(object, thisJoinPoint, thisJoinPoint.getArgs());
    }

    public final static SoftHashMap<Object, Object> lazyTwinManualExecute = new SoftHashMap<Object, Object>();
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
/*    @Around("execution(@lsfusion.server.caches.TwinManualLazy * *.*(..)) && target(object)")
    public Object callTwinManualMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        return lazyTwinManualExecute(object, thisJoinPoint, thisJoinPoint.getArgs());
    }*/
    
    @Around("execution(@lsfusion.server.caches.ParamTwinLazy * *.*(..)) && target(object)")
    // с call'ом есть баги
    public Object callParamTwinMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        Object[] args = thisJoinPoint.getArgs();
        Object[] switchArgs = new Object[args.length];
        switchArgs[0] = object;
        System.arraycopy(args, 1, switchArgs, 1, args.length - 1);

        return lazyTwinExecute(args[0], thisJoinPoint, switchArgs);
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
                                ((twin1 == ((TwinsCall) o).twin1 && twin2 == ((TwinsCall) o).twin2) ||
                                 (twin2 == ((TwinsCall) o).twin1 && twin1 == ((TwinsCall) o).twin2));
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(twin1) + System.identityHashCode(twin2);
        }
    }

    static class Twins {
        WeakIdentityHashSet<Object> objects;
        Set<Twins> diff;

        Twins(Object object) {
            objects = new WeakIdentityHashSet<Object>(object);
            diff = SetFact.mAddRemoveSet();
        }
    }

    static class TwinsMap extends WeakIdentityHashMap<Object, Twins> {

        Twins getTwins(Object object) {
            Twins twins = get(object);
            if (twins == null) {
                twins = new Twins(object);
                put(object, twins);
            }
            return twins;
        }

        public boolean call(ProceedingJoinPoint thisJoinPoint, Object object1, Object object2) throws Throwable {
            Twins twins1 = getTwins(object1);
            Twins twins2 = getTwins(object2);

            if (twins1.equals(twins2))
                return true;

            if (twins1.diff.contains(twins2))
                return false;

            if ((Boolean) thisJoinPoint.proceed()) {
                // "перекидываем" все компоненты в первую
                for (Object object : twins2.objects)
                    put(object, twins1);
                // сливаем компоненты, сами objects и diff
                twins1.objects.addAll(twins2.objects);
                // сливаем diff'ы
                twins1.diff.addAll(twins2.diff);
                for (Twins eqd : twins2.diff) { // заменяем equal2 на equal1
                    eqd.diff.remove(twins2);
                    eqd.diff.add(twins1);
                }
                return true;
            } else {
//                assert false;
                
                twins1.diff.add(twins2);
                twins2.diff.add(twins1);
                return false;
            }
        }
    }

    public static TwinsMap cacheTwins = new TwinsMap();

    @Around("execution(boolean lsfusion.base.TwinImmutableObject.twins(lsfusion.base.TwinImmutableObject)) && target(object) && args(twin)")
    // с call'ом есть баги
    public Object callTwins(ProceedingJoinPoint thisJoinPoint, GroupExpr object, AbstractSourceJoin twin) throws Throwable {
        return cacheTwins.call(thisJoinPoint, object, twin);
    }
}
