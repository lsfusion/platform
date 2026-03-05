package lsfusion.server.logics.action.flow;

import lsfusion.server.base.controller.stack.NestedThreadException;
import lsfusion.server.base.controller.stack.ThrowableWithStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledFutureService {
    private ScheduledExecutorService executor;
    private List<ScheduledFuture> futures;

    public ScheduledFutureService(ScheduledExecutorService executor, List<ScheduledFuture> futures) {
        this.executor = executor;
        this.futures = futures;
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    public void addFuture(ScheduledFuture future) {
        futures.add(future);
    }

    public NestedThreadException await() throws InterruptedException {
        List<ThrowableWithStack> throwables = new ArrayList<>();
        if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
            for (ScheduledFuture future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof RuntimeExceptionWithStack) {
                        throwables.add(new ThrowableWithStack(e.getCause().getCause(), ((RuntimeExceptionWithStack) e.getCause()).lsfStack));
                    }
                }
            }
        }
        return throwables.isEmpty() ? null : new NestedThreadException(throwables.toArray(new ThrowableWithStack[0]));
    }

    public void shutdown() {
        executor.shutdown();
    }
}