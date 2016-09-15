package lsfusion.server.stack;

import lsfusion.base.BaseUtils;
import lsfusion.base.ConcurrentWeakHashMap;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.MapFact;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.HandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.profiler.ProfileObject;
import lsfusion.server.profiler.Profiler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.ConcurrentModificationException;
import java.util.ListIterator;
import java.util.Stack;

import static lsfusion.server.profiler.Profiler.PROFILER_ENABLED;

@Aspect
public class ExecutionStackAspect {
    private static ConcurrentWeakHashMap<Thread, Stack<ExecutionStackItem>> executionStack = MapFact.getGlobalConcurrentWeakHashMap();
    private static ThreadLocal<String> threadLocalExceptionStack = new ThreadLocal<>();
    
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

    @Around("execution(public * lsfusion.interop.form.RemoteFormInterface.*(..))")
    public Object execute(ProceedingJoinPoint joinPoint) throws Throwable {
        RMICallStackItem item = new RMICallStackItem(joinPoint);
        return processStackItem(joinPoint, item);
    }
    
    public static ThreadLocal<Long> sqlTime = new ThreadLocal<>();
    public static ThreadLocal<Long> userInteractionTime = new ThreadLocal<>();

    // тут важно что цикл жизни ровно в стеке, иначе утечку можем получить
    private Object processStackItem(ProceedingJoinPoint joinPoint, ExecutionStackItem item) throws Throwable {
        assert item != null;
        
        boolean pushedMessage = false;
        Stack<ExecutionStackItem> stack = getOrInitStack();
        
        stack.push(item);
        if (presentItem(item)) {
            ThreadLocalContext.pushActionMessage(item);
            pushedMessage = true;
        }
        
        try {
            long start = 0;
            if (PROFILER_ENABLED && isProfileStackItem(item)) {
                sqlTime.set(0L);
                userInteractionTime.set(0L);
                start = System.nanoTime();
            }
            
            Object result = joinPoint.proceed();
            
            if (start > 0) {
                long executionTime = System.nanoTime() - start;
                FormInstance formInstance = ThreadLocalContext.getFormInstance();
                FormEntity form = formInstance != null ? formInstance.entity : null;
                Profiler.increase(item.profileObject, getProfileObject(getUpperProfileStackItem(item)), ThreadLocalContext.getCurrentUser(), form, executionTime, sqlTime.get(), userInteractionTime.get());
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
            stack.pop();
            if (pushedMessage) {
                ThreadLocalContext.popActionMessage(item);
            }
        }
    }
    
    private boolean isProfileStackItem(ExecutionStackItem item) {
        return item.profileObject != null;
    }
    
    private ExecutionStackItem getUpperProfileStackItem(ExecutionStackItem sourceItem) {
        Stack<ExecutionStackItem> stack = getStack();
        if(stack != null) {
            ListIterator<ExecutionStackItem> itemListIterator = stack.listIterator(stack.indexOf(sourceItem));
            while (itemListIterator.hasPrevious()) {
                ExecutionStackItem item = itemListIterator.previous();
                if (isProfileStackItem(item)) {
                    return item;
                }
            }
        }
        return null;
    }
    
    private ProfileObject getProfileObject(ExecutionStackItem item) {
        return item != null ? item.profileObject : null;    
    }

    public Stack<ExecutionStackItem> getOrInitStack() {
        Stack<ExecutionStackItem> stack = getStack(Thread.currentThread());
        if (stack == null) {
            stack = new Stack<>();
            executionStack.put(Thread.currentThread(), stack);
        }
        return stack;
    }

    public static Stack<ExecutionStackItem> getStack() {
        return getStack(Thread.currentThread());
    }
    
    public static Stack<ExecutionStackItem> getStack(Thread thread) {
        return executionStack.get(thread);
    }

    public static void setStackString(String exceptionStackString) {
        String stackString = getStackString();
        if (stackString != null) { // RMI-часть стека тоже добавляем к стеку ошибки, получаемого из ContextAwareThread
            if (exceptionStackString != null) {
                exceptionStackString += "\n";
            }
            exceptionStackString += stackString;
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
        String result = "";
        Stack<ExecutionStackItem> stack = getStack(thread);
        if (stack != null) {
            if(checkConcurrent) {
                while (true) {
                    try {
                        result = getStackString(stack, cut);
                        break;
                    } catch (ConcurrentModificationException ignored) {
                    }
                }
            } else {
                result = getStackString(stack, cut);
            }
        }
        return BaseUtils.nullEmpty(result);    
    }

    private static String getStackString(Stack<ExecutionStackItem> stack, boolean cut) {
        String result = "";
        ListIterator<ExecutionStackItem> itemListIterator = stack.listIterator(stack.size());
        boolean lastActionFound = false;
        while (itemListIterator.hasPrevious()) {
            ExecutionStackItem item = itemListIterator.previous();
            if (presentItem(item) || !lastActionFound) {
                if (!result.isEmpty()) {
                    result += "\n";
                }

                if (isLSFAction(item) && !lastActionFound) {
                    lastActionFound = true;
                    result += getLastActionString(stack, (ExecuteActionStackItem) item, cut);
                } else {
                    result += cut ? trim(item.toString(), 1000) : item;
                }
            }
        }
        return result;
    }
    
    // для последнего action'а в стеке ищем вверх по стеку первый action с именем (до action'а с IN_DELEGATE)
    private static String getLastActionString(Stack<ExecutionStackItem> stack, ExecuteActionStackItem lastAction, boolean cut) {
        ListIterator<ExecutionStackItem> itemListIterator = stack.listIterator(stack.indexOf(lastAction) + 1);
        while (itemListIterator.hasPrevious()) {
            ExecutionStackItem item = itemListIterator.previous();
            if (isLSFAction(item)) {
                ExecuteActionStackItem actionItem = (ExecuteActionStackItem) item;
                if (actionItem != lastAction && actionItem.isInDelegate()) {
                    break;
                }
                
                if (actionItem.getCanonicalName() != null) {
                    lastAction.setPropertyName(actionItem.getCaption() + " - " + actionItem.getCanonicalName());
                    break;
                }
            }
        }
        return cut ? trim(lastAction.toString(), 1000) : lastAction.toString();
    }

    public static String getProgressBarLastActionString() {
        String result = "";
        Stack<ExecutionStackItem> stack = getStack();
        if(stack != null) {
            ListIterator<ExecutionStackItem> itemListIterator = stack.listIterator(stack.size());
            while (itemListIterator.hasPrevious()) {
                ExecutionStackItem item = itemListIterator.previous();
                if (isLSFAction(item)) {
                    result = getLastActionString(stack, (ExecuteActionStackItem) item, false);
                    break;
                } else {
                    result = item.toString();
                }
            }
        }
        return result;
    }

    private static String trim(String value, int length) {
        return value == null ? null : value.substring(0, Math.min(value.length(), length));
    }

    private static boolean presentItem(ExecutionStackItem item) {
        return !isLSFAction(item) || ((ExecuteActionStackItem) item).isInDelegate();
    }
    
    private static boolean isLSFAction(ExecutionStackItem item) {
        return item instanceof ExecuteActionStackItem;
    }
}
                                                      