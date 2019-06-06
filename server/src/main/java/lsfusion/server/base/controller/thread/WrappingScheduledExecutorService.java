package lsfusion.server.base.controller.thread;

import java.util.concurrent.*;

public abstract class WrappingScheduledExecutorService extends WrappingExecutorService
        implements ScheduledExecutorService {
    final ScheduledExecutorService delegate;

    protected WrappingScheduledExecutorService(ScheduledExecutorService delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    public final ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return delegate.schedule(wrapTask(command), delay, unit);
    }

    @Override
    public final <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
        return delegate.schedule(wrapTask(task), delay, unit);
    }

    @Override
    public final ScheduledFuture<?> scheduleAtFixedRate(
            Runnable command, long initialDelay, long period, TimeUnit unit) {
        return delegate.scheduleAtFixedRate(wrapTask(command), initialDelay, period, unit);
    }

    @Override
    public final ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return delegate.scheduleWithFixedDelay(wrapTask(command), initialDelay, delay, unit);
    }

    public String getThreadPoolInfo() {
        return delegate instanceof ScheduledThreadPoolExecutor ?
                String.format("Active threads: %s, total threads: %s",
                        ((ScheduledThreadPoolExecutor) delegate).getActiveCount(), ((ScheduledThreadPoolExecutor) delegate).getCorePoolSize()) : null;
    }
}
