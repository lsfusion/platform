package lsfusion.server.stack;

import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.HandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.profiler.ExecutionTimeCounter;
import lsfusion.server.profiler.ProfileObject;
import lsfusion.server.profiler.Profiler;
import lsfusion.server.remote.ContextAwarePendingRemoteObject;
import lsfusion.server.remote.RemoteContextAspect;
import lsfusion.server.remote.RemoteForm;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static lsfusion.server.profiler.Profiler.PROFILER_ENABLED;

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
    
    @Around("execution(lsfusion.server.logics.property.actions.flow.FlowResult lsfusion.server.logics.property.ActionProperty.execute(lsfusion.server.logics.property.ExecutionContext))")
    public Object execution(final ProceedingJoinPoint joinPoint) throws Throwable {
        ExecuteActionStackItem item = new ExecuteActionStackItem(joinPoint);
        return processStackItem(joinPoint, item);
    }

    @Around("execution(@lsfusion.server.stack.StackMessage * *.*(..))")
    public Object callTwinMethod(ProceedingJoinPoint thisJoinPoint) throws Throwable {
        AspectStackItem item = new AspectStackItem(thisJoinPoint);
        return processStackItem(thisJoinPoint, item);
    }

    @Around("execution(@lsfusion.server.stack.StackProgress * *.*(..))")
    public Object callTwinMethod2(ProceedingJoinPoint thisJoinPoint) throws Throwable {
        ProgressStackItem item = new ProgressStackItem(thisJoinPoint);
        return processStackItem(thisJoinPoint, item);
    }

    @Around(RemoteContextAspect.allUserRemoteCalls)
    public Object execute(ProceedingJoinPoint joinPoint, ContextAwarePendingRemoteObject target) throws Throwable {
        assert target == joinPoint.getTarget();
        RMICallStackItem item = new RMICallStackItem(joinPoint, target);
        return processStackItem(joinPoint, item);
    }
    
    // тут важно что цикл жизни ровно в стеке, иначе утечку можем получить
    private Object processStackItem(ProceedingJoinPoint joinPoint, ExecutionStackItem item) throws Throwable {
        assert item != null;
        
        StackAndTime stackAndTime = pushStackItem(item);
        
        try {
            ExecutionTimeCounter executionTimeCounter = null;
            long start = 0;
            long sqlStart = 0;
            long uiStart = 0;
            if (PROFILER_ENABLED && isProfileStackItem(item)) {
                FormInstance formInstance = ThreadLocalContext.getFormInstance();
                FormEntity form = formInstance != null ? formInstance.entity : null;
                boolean checked = Profiler.checkUserForm(ThreadLocalContext.getCurrentUser(), form);
                
                if (checked) {
                    executionTimeCounter = executionTime.get();
                    if (executionTimeCounter == null) {
                        executionTimeCounter = new ExecutionTimeCounter();
                        executionTime.set(executionTimeCounter);
                    }

                    start = System.nanoTime();
                    sqlStart = executionTimeCounter.sqlTime;
                    uiStart = executionTimeCounter.userInteractionTime;
                }
            }

            Object result = joinPoint.proceed();

            if (start > 0 && PROFILER_ENABLED) {
                long executionTime = System.nanoTime() - start;
                FormInstance formInstance = ThreadLocalContext.getFormInstance();
                Stack<ExecutionStackItem> stack = stackAndTime.stack.getUnsync();
                assert stack.indexOf(item) == stack.size() - 1;
                Profiler.increase(
                        item.profileObject, 
                        getUpperProfileObject(stack),
                        ThreadLocalContext.getCurrentUser(),
                        formInstance != null ? formInstance.entity : null, 
                        executionTime, 
                        executionTimeCounter.sqlTime - sqlStart, 
                        executionTimeCounter.userInteractionTime - uiStart
                );
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
    
    public static String getExceptionStackString() {
        String result = threadLocalExceptionStack.get();
        threadLocalExceptionStack.set(null);
        return result != null ? result : getStackString();
    }
    
    public static String getStackString() {
        return getStackString(Thread.currentThread(), false, false); // не concurrent по определению
    }


    public static String getExStackTrace() {
        return getStackString() + '\n' + ExceptionUtils.getStackTrace();
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
                                                      