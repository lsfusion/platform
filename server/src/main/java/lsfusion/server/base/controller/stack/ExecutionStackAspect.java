package lsfusion.server.base.controller.stack;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.ReflectionUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentWeakHashMap;
import lsfusion.base.lambda.ExceptionRunnable;
import lsfusion.interop.ProgressBar;
import lsfusion.server.base.caches.CacheStats;
import lsfusion.server.base.controller.remote.context.ContextAwarePendingRemoteObject;
import lsfusion.server.base.controller.remote.context.RemoteContextAspect;
import lsfusion.server.base.controller.remote.manager.RmiServer;
import lsfusion.server.base.controller.remote.stack.RmiCallStackItem;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.HandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.stack.ExecuteActionStackItem;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.profiler.ExecutionTimeCounter;
import lsfusion.server.physics.admin.profiler.ProfileObject;
import lsfusion.server.physics.admin.profiler.Profiler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static lsfusion.server.physics.admin.profiler.Profiler.PROFILER_ENABLED;

@Aspect
public class ExecutionStackAspect {

    // String or ProgressBar or first Boolean 
    private static List<Object> getMessageList(Stack<ExecutionStackItem> stack, boolean addCancelable) {
        List<Object> result = new ArrayList<>();
        boolean lastLSFActionFound = false;
        for (int i=stack.size()-1;i>=0;i--) {
            ExecutionStackItem stackItem = stack.get(i);
            if (addCancelable && stackItem.isCancelable())
                result.add(true);

            // игнорируем lsf элементы стека, которые не создают новый стек выполнения и не являются последними (предполагается что их можно понять из нижнего элемента)
            if(stackItem instanceof ExecuteActionStackItem) {
                ExecuteActionStackItem actionStackItem = (ExecuteActionStackItem) stackItem;
                if(actionStackItem.hasNoDebugInfo()) // ignore system actions (that have no debugInfo)
                    continue;
                if(lastLSFActionFound) {
                    if (!actionStackItem.isInDelegate()) // need actions only with "stack jumps" (EXEC, EVAL, APPLY, etc.), others can be figured out of last lsf action
                        continue;
                } else
                    lastLSFActionFound = true;
            }
                
            ProgressBar progress = stackItem.getProgress();
            result.add(progress != null ? progress : stackItem.toString());
        }
        return result;
    }

    public static void take(ExceptionRunnable<InterruptedException> runnable) throws InterruptedException {
        long startTime = 0;
        ExecutionTimeCounter counter = ExecutionStackAspect.executionTime.get();
        if (counter != null)
            startTime = System.nanoTime();

        try {
            runnable.run();
        } finally {
            if (counter != null)
                counter.addUI(System.nanoTime() - startTime);
        }
    }
    public static void take(BlockingQueue sync) throws InterruptedException {
        take(sync::take);
    }

    public static String getActionMessage(Set<Thread> threads) {
        return BaseUtils.toString(getMessageList(threads), "\n");
    }
    
    public static List<Object> getMessageList(Set<Thread> threads) {
        return getStackList(threads, true, true);
    }
    
    public static List<Object> getStackList(Set<Thread> threads, boolean checkConcurrent, boolean addCancelable) {
        List<ThreadStackDump> list = getSortedThreadStacks(threads, checkConcurrent);

        List<Object> result = new ArrayList<>();
        for(ThreadStackDump entry : list) {
            List<Object> messageStack = getMessageList(entry.stack, addCancelable);
            for(int i=messageStack.size()-1;i>=0;i--) // добавляем в обратном порядке (из стека делаем список)
                result.add(messageStack.get(i));
        }
        return result;
    }

    public static Thread getLastThread(Set<Thread> threads) {
        List<ThreadStackDump> list = getSortedThreadStacks(threads, true);
        return list.isEmpty() ? null : list.get(list.size() - 1).thread;
    }

    public static ProgressStackItem pushProgressStackItem(String message, Integer progress, Integer total) {
        ProgressStackItem progressStackItem = new ProgressStackItem(message, progress, total);
        pushStackItem(progressStackItem);
        return progressStackItem;
    }

    public static void popProgressStackItem(ExecutionStackItem stackItem) {
        popStackItem(null);
    }

    private static class ThreadStackDump implements Comparable<ThreadStackDump> {
        public final Thread thread;
        public final Stack<ExecutionStackItem> stack;
        public final long time;

        public ThreadStackDump(Thread thread, Stack<ExecutionStackItem> stack, long time) {
            this.thread = thread;
            this.stack = stack;
            this.time = time;
        }

        @Override
        public int compareTo(ThreadStackDump o) {
            return Long.compare(time, o.time);
        }
    }

    public static List<ThreadStackDump> getSortedThreadStacks(Set<Thread> threads, boolean checkConcurrent) {
        List<ThreadStackDump> list = new ArrayList<>(); 
        for(Thread thread : threads) {
            StackAndTime stackAndTime = executionStack.get(thread);
            if(stackAndTime != null) {
                Stack<ExecutionStackItem> stack = checkConcurrent ? stackAndTime.stack.getSync() : stackAndTime.stack.getUnsync();
                if(!stack.isEmpty())
                    list.add(new ThreadStackDump(thread, stack, stackAndTime.time));
            }
        }
        Collections.sort(list);
        return list;
    }

    // нжуен для синхронизации
    // пока ничего не синхронизируем а ловим ConcurrentModification в асинхронных вызовах
    private static class SyncStack {
        
        private final Stack<ExecutionStackItem> stack = new Stack<>();
        
        public ExecutionStackItem push(ExecutionStackItem item) {
            return stack.push(item);
        }
        public ExecutionStackItem pop() {
            return stack.pop();
        }
        
        private Stack<ExecutionStackItem> getSync() { // вызывается из асинхронных read потоков
            while(true) {
                try {
                    Stack<ExecutionStackItem> dump = new Stack<>();
                    for(ExecutionStackItem stackItem : stack)
                        dump.add(stackItem);
                    return dump;
                } catch (ConcurrentModificationException e) {                    
                }
            }
        }
        
        private Stack<ExecutionStackItem> getUnsync() { // вызывается из синхронных write потоков
            return stack;
        }
    }

    private static class StackAndTime {
        public SyncStack stack = new SyncStack();
        public Long time = System.currentTimeMillis(); // last call
    }
    private static ConcurrentWeakHashMap<Thread, StackAndTime> executionStack = MapFact.getGlobalConcurrentWeakHashMap();
    
    private static ThreadLocal<String> threadLocalExceptionStack = new ThreadLocal<>();
    public static ThreadLocal<ExecutionTimeCounter> executionTime = new ThreadLocal<>();
    
    @Around("execution(lsfusion.server.logics.action.flow.FlowResult lsfusion.server.logics.action.Action.execute(lsfusion.server.logics.action.controller.context.ExecutionContext))")
    public Object execution(final ProceedingJoinPoint joinPoint) throws Throwable {
        ExecuteActionStackItem item = new ExecuteActionStackItem(joinPoint);
        return processStackItem(joinPoint, item);
    }

    @Around("execution(@lsfusion.server.base.controller.stack.StackMessage * *.*(..))")
    public Object callTwinMethod(ProceedingJoinPoint thisJoinPoint) throws Throwable {
        AspectStackItem item = new AspectStackItem(thisJoinPoint);
        return processStackItem(thisJoinPoint, item);
    }

    @Around("execution(@lsfusion.server.base.controller.stack.StackProgress * *.*(..))")
    public Object callTwinMethod2(ProceedingJoinPoint thisJoinPoint) throws Throwable {
        ProgressStackItem item = new ProgressStackItem(thisJoinPoint);
        return processStackItem(thisJoinPoint, item);
    }

    @Around(RemoteContextAspect.allUserRemoteCalls)
    public Object execute(ProceedingJoinPoint joinPoint, Object target) throws Throwable {
        assert target == joinPoint.getTarget();
        Object profiledObject;
        if(target instanceof ContextAwarePendingRemoteObject)
            profiledObject = ((ContextAwarePendingRemoteObject) target).getProfiledObject();
        else
            profiledObject = ((RmiServer) target).getEventName();
        
        RmiCallStackItem item = new RmiCallStackItem(joinPoint, profiledObject);
        return processStackItem(joinPoint, item);
    }

    private static Map<Long, Boolean> explainAppEnabled = MapFact.getGlobalConcurrentHashMap();
    
    public static void setExplainAppEnabled(Long user, Boolean enabled) {
        explainAppEnabled.put(user, enabled != null && enabled);
    }

    public static boolean isExplainAppEnabled() {
        Long currentUser = ThreadLocalContext.getCurrentUser();
        if (currentUser == null)
            return false;
        Boolean ett = explainAppEnabled.get(currentUser);
        return ett != null && ett;
    }

    public static boolean isProfilerEnabled() {
        if(!PROFILER_ENABLED)
            return false;

        return Profiler.checkUserForm(ThreadLocalContext.getCurrentUser(), ThreadLocalContext.getCurrentForm());
    }

    private static boolean allocationBytesSupplierCalculated = false; // cache
    private static Supplier<Long> allocationBytesSupplier = null;
    private static Supplier<Long> getAllocationBytesSupplier() {
        if(!allocationBytesSupplierCalculated) {
            Class threadMXBeanClass = ReflectionUtils.classForName("com.sun.management.ThreadMXBean");
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            if(threadMXBeanClass != null && threadMXBeanClass.isInstance(threadMXBean))
                allocationBytesSupplier = () -> ReflectionUtils.getMethodValue(threadMXBeanClass, threadMXBean, "getThreadAllocatedBytes", new Class[]{long.class}, new Object[] {Thread.currentThread().getId()});
            else
                allocationBytesSupplier = () -> 0L;
            allocationBytesSupplierCalculated = true;
        }
        return allocationBytesSupplier;
    }

    // тут важно что цикл жизни ровно в стеке, иначе утечку можем получить
    private Object processStackItem(ProceedingJoinPoint joinPoint, ExecutionStackItem item) throws Throwable {
        assert item != null;
        
        StackAndTime stackAndTime = pushStackItem(item);
        
        try {
            boolean EXPLAINAPP_ENABLED = isExplainAppEnabled();
            boolean PROFILER_ENABLED = isProfilerEnabled() && isProfileStackItem(item);

            boolean ANALYZE_ENABLED = PROFILER_ENABLED || EXPLAINAPP_ENABLED;

            ExecutionTimeCounter executionTimeCounter = null;
            boolean topStack = false;
            long start = 0;
            long sqlStart = 0;
            long uiStart = 0;
            boolean allocatedBytesEnabled = false;
            Supplier<Long> allocationBytesSupplier = null;
            long allocatedBytesOnStart = 0;
            Map<CacheStats.CacheType, Long> hitStatsOnStart = null;
            Map<CacheStats.CacheType, Long> missedStatsOnStart = null;
            if (ANALYZE_ENABLED) {
                executionTimeCounter = executionTime.get();
                if (executionTimeCounter == null) {
                    topStack = true;
                    executionTimeCounter = new ExecutionTimeCounter();
                    executionTime.set(executionTimeCounter);
                }

                start = System.nanoTime();
                sqlStart = executionTimeCounter.sqlTime;
                uiStart = executionTimeCounter.userInteractionTime;

                if(EXPLAINAPP_ENABLED) {
                    allocatedBytesEnabled = Settings.get().getExplainAllocatedBytesThreshold() > 0;
                    if(allocatedBytesEnabled) // optimization since there can be performance issues with getThreadAllocatedBytes
                        allocationBytesSupplier = getAllocationBytesSupplier();
                    else
                        allocationBytesSupplier = () -> 0L;

                    allocatedBytesOnStart = allocationBytesSupplier.get();

                    long currentThreadId = Thread.currentThread().getId();
                    hitStatsOnStart = CacheStats.getCacheHitStats(currentThreadId);
                    missedStatsOnStart = CacheStats.getCacheMissedStats(currentThreadId);
                }
            }

            Object result = joinPoint.proceed();

            if (ANALYZE_ENABLED) {
                long totalTime = System.nanoTime() - start;
                Stack<ExecutionStackItem> stack = stackAndTime.stack.getUnsync();
                assert stack.indexOf(item) == stack.size() - 1;

                long sqlTime = executionTimeCounter.sqlTime - sqlStart;
                long userInteractionTime = executionTimeCounter.userInteractionTime - uiStart;
                long javaTime = totalTime - sqlTime - userInteractionTime;

                if(Profiler.PROFILER_ENABLED)
                    Profiler.increase(item.profileObject, getUpperProfileObject(stack), ThreadLocalContext.getCurrentUser(), ThreadLocalContext.getCurrentForm(), totalTime, sqlTime, userInteractionTime);

                if(EXPLAINAPP_ENABLED) {
                    long allocatedBytes = allocationBytesSupplier.get() - allocatedBytesOnStart;
                    javaTime = javaTime / 1000000; // because this measurements are nanos
                    sqlTime = sqlTime / 1000000;

                    boolean javaExceeded = javaTime >= Settings.get().getExplainAppThreshold();
                    boolean sqlExceeded = sqlTime >= Settings.get().getExplainThreshold();
                    boolean allocatedExceeded = allocatedBytes > Settings.get().getExplainAllocatedBytesThreshold();
                    String explainDetails = null;
                    if (javaExceeded || sqlExceeded || allocatedExceeded) {
                        BiFunction<Object, Boolean, String> highlighter = (s, exceeded) -> exceeded ? "!! " + s + " !!" : "" + s;
                        long currentThreadId = Thread.currentThread().getId();

                        if(executionTimeCounter.info == null)
                            executionTimeCounter.info = ListFact.mList();
                        // adding \t we're assuming that all upper stack elements will also exceed threshold (since the times are cumulative)
                        explainDetails = " (" +
                                "app: " + highlighter.apply(javaTime, javaExceeded) +
                                ", sql: " + highlighter.apply(sqlTime, sqlExceeded) +
                                ", sql/app ratio: " + (sqlTime + javaTime != 0 ? ((sqlTime * 100) / (sqlTime + javaTime)) + "%" : "--" ) +
                                ", allocated app: " + highlighter.apply(BusinessLogics.humanReadableByteCount(allocatedBytes), allocatedExceeded) +
                                ", cache hit ratio - " + CacheStats.getGroupedRatioString(
                                        CacheStats.diff(CacheStats.getCacheHitStats(currentThreadId), hitStatsOnStart),
                                        CacheStats.diff(CacheStats.getCacheMissedStats(currentThreadId), missedStatsOnStart)) +
                                ")";
                        executionTimeCounter.info.add(BaseUtils.replicate('\t', stack.size() - 1) + item + explainDetails);
                    }

                    if(topStack) {
                        if(executionTimeCounter.info != null && (
                                javaTime >= Settings.get().getExplainTopAppThreshold() ||
                                sqlTime >= Settings.get().getExplainTopThreshold() ||
                                (allocatedBytesEnabled && allocatedBytes >= Settings.get().getExplainTopAllocatedBytesThreshold())))
                            ServerLoggers.explainAppLogger.info(explainDetails + '\n' + executionTimeCounter.info.immutableList().reverseList().toString("\n"));
                        executionTime.set(null);
                    }
                }
            }

            return result;
        } catch (Throwable e) {
            if (!(e instanceof HandledException && ((HandledException)e).willDefinitelyBeHandled()) && threadLocalExceptionStack.get() == null) {
                String stackString = getStackString();
                if (stackString != null) {
                    threadLocalExceptionStack.set(stackString);
                }
            }
            throw e;
        } finally {
            popStackItem(stackAndTime);
        }
    }

    public static StackAndTime pushStackItem(ExecutionStackItem item) {
        Thread thread = Thread.currentThread();
        StackAndTime stackAndTime = executionStack.get(thread);
        if (stackAndTime == null) {
            stackAndTime = new StackAndTime();
            executionStack.put(thread, stackAndTime);
        }
        stackAndTime.stack.push(item);
        stackAndTime.time = System.currentTimeMillis();
        return stackAndTime;
    }

    public static void popStackItem(StackAndTime stackAndTime) {
        if(stackAndTime == null) {
            Thread thread = Thread.currentThread();
            stackAndTime = executionStack.get(thread);
        }
        stackAndTime.stack.pop();
    }

    private boolean isProfileStackItem(ExecutionStackItem item) {
        return item.profileObject != null;
    }
    
    private ProfileObject getUpperProfileObject(Stack<ExecutionStackItem> stack) {
        for (int i = stack.size() - 2; i >= 0; i--) {
            ExecutionStackItem item = stack.get(i);
            if (isProfileStackItem(item)) {
                return item.profileObject;
            }
        }
        return null;
    }

    public static void setExceptionStackString(String exceptionStackString) {
        String stackString = getStackString();
        if (!stackString.isEmpty()) {
            if (!exceptionStackString.isEmpty())
                stackString += "\n";
            exceptionStackString = stackString + exceptionStackString;
        }
        threadLocalExceptionStack.set(exceptionStackString);
    }

    public static String getExceptionStackTrace() {
        return getExceptionStackTrace(true);
    }

    public static String getExceptionStackTrace(boolean drop) {
        String result = threadLocalExceptionStack.get();
        if (drop)
            threadLocalExceptionStack.set(null);
        return result != null ? result : getStackString();
    }
    
    public static String getStackString() {
        return getStackString(Thread.currentThread(), false, false); // не concurrent по определению
    }

    public static String getExStackTrace() {
        return ExceptionUtils.getExStackTrace(ExceptionUtils.getStackTrace(), getStackString());
    }

    public static String getStackString(Thread thread, boolean checkConcurrent, boolean cut) {
        List<Object> elements = getStackList(Collections.singleton(thread), checkConcurrent, false);
        StringBuilder result = new StringBuilder();
        for(Object element : elements) {
            if (result.length() > 0)
                result.append("\n");

            String elementString = element.toString();
            if(cut)
                elementString = BaseUtils.substring(elementString, 1000);
            result.append(elementString);
        }
        return result.toString();    
    }

    private static boolean isLSFAction(ExecutionStackItem item) {
        return item instanceof ExecuteActionStackItem;
    }
}
                                                      