package lsfusion.server.logics.action.flow;

import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.logics.action.controller.context.ExecutionContext;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Server-pool dispatch: NEWTHREAD inside a NEWEXECUTOR THREADS n scope. */
public class ServerFutureService extends ScheduledFutureService {
    private final ScheduledExecutorService executor;

    public ServerFutureService(ScheduledExecutorService executor, boolean awaited) {
        super(awaited);
        this.executor = executor;
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    @Override
    protected boolean awaitReady() throws InterruptedException {
        return executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public void interruptIfPossible(ExecutionContext<?> context) {
        ThreadUtils.interruptThreadExecutor(executor, context);
    }
}
