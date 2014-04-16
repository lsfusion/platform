package lsfusion.server.logics.tasks;

import lsfusion.base.BaseUtils;
import lsfusion.server.context.ContextAwareDaemonThreadFactory;
import lsfusion.server.context.ThreadLocalContext;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskRunner {
    
    public static int availableProcessors() {
        return Runtime.getRuntime().availableProcessors() / 2; 
    }
    public static void runTask(PublicTask task, Logger logger) throws InterruptedException {
        Set<Task> initialTasks = new HashSet<Task>();
        task.markInDependencies(initialTasks);

        //Runtime.getRuntime().availableProcessors() * 2
        int nThreads = availableProcessors();
        TaskBlockingQueue taskQueue = new TaskBlockingQueue();
//        BlockingQueue<Task.PriorityRunnable> taskQueue = new PriorityBlockingQueue<Task.PriorityRunnable>();
        ExecutorService executor = new ThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                taskQueue,
                new ContextAwareDaemonThreadFactory(ThreadLocalContext.get(), "init-pool"));
//        ExecutorService executor = Executors.newSingleThreadExecutor(new ContextAwareDaemonThreadFactory(ThreadLocalContext.get(), "init-pool"));
        AtomicInteger taskCount = new AtomicInteger(0);
        final Object monitor = new Object();
        for(Task initialTask : initialTasks)
            initialTask.execute(executor, monitor, taskCount, logger, taskQueue);

        while(taskCount.get() > 0)
            synchronized (monitor) {
                monitor.wait();
            }
        executor.shutdown();
    }
}
