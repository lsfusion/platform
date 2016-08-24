package lsfusion.server.context;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class WrappingExecutorService implements ExecutorService {
    private final ExecutorService delegate;

    protected WrappingExecutorService(ExecutorService delegate) {
        this.delegate = checkNotNull(delegate);
    }

    /**
     * Wraps a {@code Callable} for submission to the underlying executor. This
     * method is also applied to any {@code Runnable} passed to the default
     * implementation of {@link #wrapTask(Runnable)}.
     */
    protected abstract <T> Callable<T> wrapTask(Callable<T> callable);

    /**
     * Wraps a {@code Runnable} for submission to the underlying executor. The
     * default implementation delegates to {@link #wrapTask(Callable)}.
     */
    protected Runnable wrapTask(Runnable command) {
        final Callable<Object> wrapped = wrapTask(
                Executors.callable(command, null));
        return new Runnable() {
            @Override public void run() {
                try {
                    wrapped.call();
                } catch (Exception e) {
                    Throwables.propagate(e);
                }
            }
        };
    }

    /**
     * Wraps a collection of tasks.
     *
     * @throws NullPointerException if any element of {@code tasks} is null
     */
    private final <T> ImmutableList<Callable<T>> wrapTasks(
            Collection<? extends Callable<T>> tasks) {
        ImmutableList.Builder<Callable<T>> builder = ImmutableList.builder();
        for (Callable<T> task : tasks) {
            builder.add(wrapTask(task));
        }
        return builder.build();
    }

    // These methods wrap before delegating.
    @Override
    public final void execute(Runnable command) {
        delegate.execute(wrapTask(command));
    }

    @Override
    public final <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(wrapTask(checkNotNull(task)));
    }

    @Override
    public final Future<?> submit(Runnable task) {
        return delegate.submit(wrapTask(task));
    }

    @Override
    public final <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(wrapTask(task), result);
    }

    @Override
    public final <T> List<Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(wrapTasks(tasks));
    }

    @Override
    public final <T> List<Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(wrapTasks(tasks), timeout, unit);
    }

    @Override
    public final <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return delegate.invokeAny(wrapTasks(tasks));
    }

    @Override
    public final <T> T invokeAny(
            Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(wrapTasks(tasks), timeout, unit);
    }

    // The remaining methods just delegate.

    @Override
    public final void shutdown() {
        delegate.shutdown();
    }

    @Override
    public final List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public final boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public final boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public final boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }
}

