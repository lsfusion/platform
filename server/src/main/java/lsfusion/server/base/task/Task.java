package lsfusion.server.base.task;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.stack.StackNewThread;
import lsfusion.server.base.controller.stack.ThisMessage;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static lsfusion.server.base.task.TaskRunner.ThrowableConsumer;

public abstract class Task {

    public static Comparator<PriorityRunnable> leastEstimated = (o1, o2) -> Task.compareTo(o2.getBaseComplexity(), o1.getBaseComplexity(), o2, o1);
    public Map<Task, Object> dependsFrom = new HashMap<>();
    protected Integer dependsToProceed;
    protected long dependComplexity;
    protected boolean finalizedComplexity;

    public boolean ignoreExceptions() {
        return false;
    }

    public boolean isLoggable() {
        return true;
    }

    public abstract String getCaption();

    public boolean isEndLoggable() {
        return false;
    }

    public String getEndCaption() {
        return getCaption() + " ended";
    }

    protected long getBaseComplexity() {
        return 1;
    }

    protected long getComplexity() {
        return getBaseComplexity() + dependComplexity;
    }

    public void markInDependencies(Set<Task> initialTasks) {
        // initialTasks а не executor чтобы не synchronize'ть dependsFrom
        if (dependsToProceed != null) {
            return;
        }

        dependsToProceed = 0;
        Set<Task> allDependencies = getAllDependencies();
        if (allDependencies.isEmpty()) {
            initialTasks.add(this);
        } else {
            for (Task depend : allDependencies) {
                depend.markInDependencies(initialTasks);
                addDependency(depend);
            }
        }
    }

    public abstract Set<Task> getAllDependencies();

    @StackNewThread
    @StackMessage("scheduler.form.scheduled.task")
    @ThisMessage
    public abstract void run(Logger logger);

    @Override
    public String toString() {
        return getCaption();
    }

    // не так важно какой
    public void dependProceeded(BusinessLogics BL, Executor executor, ExecutionContext context, Object monitor, AtomicInteger taskCount, Logger logger,
                                final Task taskProceeded, TaskBlockingQueue taskQueue, ThrowableConsumer throwableConsumer, Integer propertyTimeout) {
        int newDepends;
        synchronized (this) {
            newDepends = dependsToProceed - 1;
            dependsToProceed = newDepends;
        }
//        System.out.println("DEPPROC " + newDepends + " " + getCaption() + " AFYER " + taskProceeded.getCaption());
        if (newDepends == 0) {
            execute(BL, executor, context, monitor, taskCount, logger, taskQueue, throwableConsumer, propertyTimeout);
        }
    }

    public void execute(final BusinessLogics BL, final Executor executor, final ExecutionContext context, final Object monitor, final AtomicInteger taskCount, final Logger logger,
                        final TaskBlockingQueue taskQueue, final ThrowableConsumer throwableConsumer, final Integer propertyTimeout) {
        logTaskCount(logger, taskCount.incrementAndGet());
        executor.execute(new PriorityRunnable() {
            protected void aspectRun() {
                try {
                    taskQueue.ensurePolled(this);
                    proceed(BL, executor, context, monitor, taskCount, logger, taskQueue, throwableConsumer, propertyTimeout);
                } catch (Throwable t) {
                    ThrowableWithStack throwableWithStack = new ThrowableWithStack(t);
                    if(!ignoreExceptions())
                        throwableConsumer.consume(throwableWithStack);
                    else
                        throwableWithStack.log(getCaption(), logger);
                } finally {
                    taskQueue.removePolled(this);
                    int taskInQueue = taskCount.decrementAndGet();
                    logTaskCount(logger, taskInQueue);
                    if (taskInQueue == 0) {
                        synchronized (monitor) {
                            monitor.notify();
                        }
                    }
                }
            }
        });
    }

    public void addDependency(Task task) {
        addDependency(task, true);
    }

    public void addDependency(Task task, boolean updateComplexity) {
        task.dependsFrom.put(this, 0);
        dependsToProceed++;
        if (updateComplexity) {
            assert !task.finalizedComplexity;
            task.dependComplexity = addComplexity(task.dependComplexity, getComplexity());
        }
    }

    public void proceed(BusinessLogics BL, Executor executor, ExecutionContext context, Object monitor, AtomicInteger taskCount, final Logger logger,
                        TaskBlockingQueue taskQueue, ThrowableConsumer throwableConsumer, Integer propertyTimeout) throws InterruptedException, SQLException, SQLHandledException, ExecutionException {
        if (isLoggable()) {
            String caption = getCaption();
            if(caption != null)
                logger.info(caption);
        }
        if(propertyTimeout == null) {
            run(logger);
        } else {
            ExecutorService service = ExecutorFactory.createTaskMirrorSyncService(BaseUtils.immutableCast(context));
            final Result<Thread> thread = new Result<>();
            Future future = service.submit(() -> {
                thread.set(Thread.currentThread());
                try {
                    Task.this.run(logger);
                } finally {
                    thread.set(null);
                }
            });
            service.shutdown();

            try {
                future.get(propertyTimeout, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                ThreadUtils.interruptThread(BL.getDbManager(), thread.result, future);
            }
        }

        if(isEndLoggable())
            logger.info(getEndCaption());

        for (Task from : dependsFrom.keySet()) {
            from.dependProceeded(BL, executor, context, monitor, taskCount, logger, this, taskQueue, throwableConsumer, propertyTimeout);
        }
    }

    private static void logTaskCount(Logger logger, int count) {
        if (1 != 1 && TaskRunner.availableProcessors() > count) {
            logger.info("low tasks in queue :" + count);
        }
    }

    public static int compareTo(long l1, long l2, Object o1, Object o2) {
        if (l1 > l2) {
            return 1;
        }
        if (l1 < l2) {
            return -1;
        }
        return Integer.compare(System.identityHashCode(o1), System.identityHashCode(o2));
    }

    private static long addComplexity(long oldComplexity, long newComplexity) {
        return BaseUtils.max(oldComplexity, newComplexity);
    }

    public abstract class PriorityRunnable extends ExecutorFactory.AspectRunnable implements Comparable<PriorityRunnable> {

        public long getComplexity() {
            finalizedComplexity = true;
            return Task.this.getComplexity();
        }

        public int compareTo(PriorityRunnable o) {
            return Task.compareTo(getComplexity(), o.getComplexity(), this, o);
        }

        public long getBaseComplexity() {
            return Task.this.getBaseComplexity();
        }
    }
}
