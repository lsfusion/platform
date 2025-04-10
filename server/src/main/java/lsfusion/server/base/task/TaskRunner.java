package lsfusion.server.base.task;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.server.base.controller.stack.NestedThreadException;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
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
        runTask(task, ServerLoggers.startLogger, null, null, null, null);
    }

    public void runTask(PublicTask task, Logger logger, Integer threadCount, Long propertyTimeout, ExecutionContext<ClassPropertyInterface> context, Runnable onInterrupted) {
        Set<Task> initialTasks = new HashSet<>();
        task.markInDependencies(initialTasks);

        try {
        //Runtime.getRuntime().availableProcessors() * 2
            int nThreads = threadCount != null && threadCount != 0 ? threadCount : availableProcessors();
            TaskBlockingQueue taskQueue = new TaskBlockingQueue();
    //        BlockingQueue<Task.PriorityRunnable> taskQueue = new PriorityBlockingQueue<Task.PriorityRunnable>();
            executor = ExecutorFactory.createTaskService(nThreads, taskQueue,  BaseUtils.immutableCast(context));

    //        ExecutorService executor = Executors.newSingleThreadExecutor(new ContextAwareDaemonThreadFactory(ThreadLocalContext.get(), "task-daemon"));
            AtomicInteger taskCount = new AtomicInteger(0);
            final Object monitor = new Object();

            final ThrowableConsumer throwableConsumer = new ThrowableConsumer();

            for (Task initialTask : initialTasks)
                initialTask.execute(BL, executor, context, monitor, taskCount, logger, taskQueue, throwableConsumer, propertyTimeout);

            while (taskCount.get() > 0)
                synchronized (monitor) {
                    monitor.wait();
                }

            List<ThrowableWithStack> errors = throwableConsumer.getThrowables();
            if (!errors.isEmpty())
                throw new NestedThreadException(errors.toArray(new ThrowableWithStack[errors.size()]));
        } catch (InterruptedException e) {
            ThreadUtils.interruptThreadExecutor(executor, context);

            if(onInterrupted != null)
                onInterrupted.run();

            throw Throwables.propagate(e);
        } finally {
            if(executor != null)
               executor.shutdown();
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
