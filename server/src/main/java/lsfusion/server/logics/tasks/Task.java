package lsfusion.server.logics.tasks;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.ThreadUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static lsfusion.server.logics.tasks.TaskRunner.ThrowableConsumer;

public abstract class Task {

    public static Comparator<PriorityRunnable> leastEstimated = new Comparator<PriorityRunnable>() {
        public int compare(PriorityRunnable o1, PriorityRunnable o2) {
            return Task.compareTo(o2.getBaseComplexity(), o1.getBaseComplexity(), o2, o1);
        }
    };
    public Map<Task, Object> dependsFrom = new HashMap<Task, Object>();
    protected Integer dependsToProceed;
    protected long dependComplexity;
    protected boolean finalizedComplexity;

    public boolean isLoggable() {
        return true;
    }

    public abstract String getCaption();

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

    public abstract void run();

    // не так важно какой
    public void dependProceeded(BusinessLogics BL, Executor executor, Object monitor, AtomicInteger taskCount, Logger logger,
                                final Task taskProceeded, TaskBlockingQueue taskQueue, ThrowableConsumer throwableConsumer, Integer propertyTimeout) {
        int newDepends;
        synchronized (this) {
            newDepends = dependsToProceed - 1;
            dependsToProceed = newDepends;
        }
//        System.out.println("DEPPROC " + newDepends + " " + getCaption() + " AFYER " + taskProceeded.getCaption());
        if (newDepends == 0) {
            execute(BL, executor, monitor, taskCount, logger, taskQueue, throwableConsumer, propertyTimeout);
        }
    }

    public void execute(final BusinessLogics BL, final Executor executor, final Object monitor, final AtomicInteger taskCount, final Logger logger,
                        final TaskBlockingQueue taskQueue, final ThrowableConsumer throwableConsumer, final Integer propertyTimeout) {
        logTaskCount(logger, taskCount.incrementAndGet());
        executor.execute(new PriorityRunnable() {
            public void run() {
                try {
                    taskQueue.ensurePolled(this);
                    proceed(BL, executor, monitor, taskCount, logger, taskQueue, throwableConsumer, propertyTimeout);
                } catch (Throwable t) {
                    throwableConsumer.consume(t);
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

    public void proceed(BusinessLogics BL, Executor executor, Object monitor, AtomicInteger taskCount, Logger logger,
                        TaskBlockingQueue taskQueue, ThrowableConsumer throwableConsumer, Integer propertyTimeout) throws InterruptedException, SQLException, SQLHandledException {
        if (isLoggable()) {
            logger.info(getCaption());
        }
        if(propertyTimeout == null) {
            run();
        } else {
            TimeoutThread timeoutThread = new TimeoutThread(this);
            timeoutThread.start();
            timeoutThread.join(propertyTimeout);
            if(timeoutThread.isAlive()) {
                ThreadUtils.interruptThread(BL.getDbManager(), timeoutThread);
            }
        }

        for (Task from : dependsFrom.keySet()) {
            from.dependProceeded(BL, executor, monitor, taskCount, logger, this, taskQueue, throwableConsumer, propertyTimeout);
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
        return ((Integer) System.identityHashCode(o1)).compareTo((Integer) System.identityHashCode(o2));
    }

    private static long addComplexity(long oldComplexity, long newComplexity) {
        return BaseUtils.max(oldComplexity, newComplexity);
    }

    public abstract class PriorityRunnable implements Runnable, Comparable<PriorityRunnable> {

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

    private class TimeoutThread extends Thread {
        Task task;

        public TimeoutThread(Task task) {
            this.task = task;
        }

        @Override
        public void run() {
            task.run();
        }
    }
}
