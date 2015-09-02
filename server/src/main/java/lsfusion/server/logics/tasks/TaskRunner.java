package lsfusion.server.logics.tasks;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.MultiCauseException;
import lsfusion.server.context.ContextAwareDaemonThreadFactory;
import lsfusion.server.context.ThreadLocalContext;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskRunner {
    ExecutorService executor;

    public static int availableProcessors() {
        return BaseUtils.max(Runtime.getRuntime().availableProcessors() / 2, 1);
    }

    public void runTask(PublicTask task, Logger logger) throws InterruptedException {
        runTask(task, logger, null);
    }

    public void runTask(PublicTask task, Logger logger, Integer threadCount) throws InterruptedException {
        Set<Task> initialTasks = new HashSet<>();
        task.markInDependencies(initialTasks);

        //Runtime.getRuntime().availableProcessors() * 2
        int nThreads = threadCount != null && threadCount != 0 ? threadCount : availableProcessors();
        TaskBlockingQueue taskQueue = new TaskBlockingQueue();
//        BlockingQueue<Task.PriorityRunnable> taskQueue = new PriorityBlockingQueue<Task.PriorityRunnable>();
        executor = new ThreadPoolExecutor(nThreads, nThreads,
                                                          0L, TimeUnit.MILLISECONDS,
                                                          taskQueue,
                                                          new ContextAwareDaemonThreadFactory(ThreadLocalContext.get(), "init-pool"));
//        ExecutorService executor = Executors.newSingleThreadExecutor(new ContextAwareDaemonThreadFactory(ThreadLocalContext.get(), "task-daemon"));
        AtomicInteger taskCount = new AtomicInteger(0);
        final Object monitor = new Object();
        
        final ThrowableConsumer throwableConsumer = new ThrowableConsumer();
        
        for (Task initialTask : initialTasks) {
            initialTask.execute(executor, monitor, taskCount, logger, taskQueue, throwableConsumer);
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
        
        List<Throwable> errors = throwableConsumer.getThrowables();
        if (!errors.isEmpty()) {
            if (errors.size() == 1) {
                throw Throwables.propagate(errors.get(0));
            } else {
                throw new MultiCauseException(errors.toArray(new Throwable[errors.size()]));
            }
        }
    }

    public void shutdownNow() {
        if(executor != null)
            executor.shutdownNow();
    }
    
    public static class ThrowableConsumer {
        private List<Throwable> throwables = Collections.synchronizedList(new ArrayList<Throwable>());
        
        public final void consume(Throwable t) {
            throwables.add(t);
        }

        public final List<Throwable> getThrowables() {
            return throwables;
        }
    }
}
