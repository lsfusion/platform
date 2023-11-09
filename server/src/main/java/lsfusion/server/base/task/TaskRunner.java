package lsfusion.server.base.task;

import lsfusion.base.BaseUtils;
import lsfusion.server.base.controller.stack.NestedThreadException;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskRunner {
    BusinessLogics BL;
    ExecutorService executor;

    public TaskRunner(BusinessLogics BL) {
        this.BL = BL;
    }

    public static int availableProcessors() {
        return BaseUtils.max(Runtime.getRuntime().availableProcessors(), 1);
    }

    // lifecycle
    public void runTask(PublicTask task) throws InterruptedException {
        runTask(task, ServerLoggers.startLogger, null, null, null);
    }

    public void runTask(PublicTask task, Logger logger, Integer threadCount, Integer propertyTimeout, ExecutionContext<ClassPropertyInterface> context) throws InterruptedException {
        Set<Task> initialTasks = new HashSet<>();
        task.markInDependencies(initialTasks);

        //Runtime.getRuntime().availableProcessors() * 2
        int nThreads = threadCount != null && threadCount != 0 ? threadCount : availableProcessors();
        TaskBlockingQueue taskQueue = new TaskBlockingQueue();
//        BlockingQueue<Task.PriorityRunnable> taskQueue = new PriorityBlockingQueue<Task.PriorityRunnable>();
        executor = ExecutorFactory.createTaskService(nThreads, taskQueue,  BaseUtils.immutableCast(context));

//        ExecutorService executor = Executors.newSingleThreadExecutor(new ContextAwareDaemonThreadFactory(ThreadLocalContext.get(), "task-daemon"));
        AtomicInteger taskCount = new AtomicInteger(0);
        final Object monitor = new Object();
        
        final ThrowableConsumer throwableConsumer = new ThrowableConsumer();
        
        for (Task initialTask : initialTasks) {
            initialTask.execute(BL, executor, context, monitor, taskCount, logger, taskQueue, throwableConsumer, propertyTimeout);
        }

        try {
            while (taskCount.get() > 0) {
                synchronized (monitor) {
                    monitor.wait();
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            throw e;
        }
        executor.shutdown();
        
        List<ThrowableWithStack> errors = throwableConsumer.getThrowables();
        if (!errors.isEmpty())
            throw new NestedThreadException(errors.toArray(new ThrowableWithStack[errors.size()]));
    }

    public void shutdownNow() {
        if(executor != null)
            executor.shutdownNow();
    }

    public void interruptThreadPoolProcesses(ExecutionContext context) {
        try {
            Field workerField = ThreadPoolExecutor.class.getDeclaredField("workers");
            workerField.setAccessible(true);
            Class workerClass = Class.forName("java.util.concurrent.ThreadPoolExecutor$Worker");

            HashSet<Object> workers = (HashSet<Object>) workerField.get(executor);
            Field threadField = workerClass.getDeclaredField("thread");
            threadField.setAccessible(true);
            for(Object worker : workers) {
                ThreadUtils.interruptThread(context, (Thread) threadField.get(worker));
            }
        } catch (Exception e) {
            ServerLoggers.systemLogger.error("Failed to kill sql processes in TaskRunner", e);
        }
    }
    
    public static class ThrowableConsumer {
        private List<ThrowableWithStack> throwables = Collections.synchronizedList(new ArrayList<>());
        
        public final void consume(ThrowableWithStack t) {
            throwables.add(t);
        }

        public final List<ThrowableWithStack> getThrowables() {
            return throwables;
        }
    }
}
