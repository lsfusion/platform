package lsfusion.server.logics.action.flow;

import lsfusion.server.base.controller.stack.NestedThreadException;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.logics.action.controller.context.ExecutionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Ambient dispatch context for NEWTHREAD inside a NEWEXECUTOR scope.
 * Tracks per-task completion futures so that a WAIT-mode enclosing scope
 * can join them at scope exit. Concrete subclasses ({@link ServerFutureService},
 * {@link ClientFutureService}) own the mode-specific resources (thread pool /
 * connection) and define shutdown / interrupt semantics.
 *
 * WAIT bound: the per-task future will eventually settle even if the
 * underlying work never runs — every {@link RemoteNavigator.Notification}
 * carries a retention deadline (set in its constructor from
 * {@code Settings.notificationCleanupPeriod}); when the deadline elapses
 * {@code sweepStaleNotifications} fires {@code onExpire}, which the dispatch
 * site overrides to settle the source exceptionally. For server-pool tasks
 * the future settles when the worker finishes.
 */
public abstract class ScheduledFutureService {
    protected final List<Future<?>> futures = Collections.synchronizedList(new ArrayList<>());

    public void addFuture(Future<?> future) {
        futures.add(future);
    }

    public NestedThreadException await() throws InterruptedException {
        if (!awaitReady()) {
            return null;
        }
        List<ThrowableWithStack> throwables = new ArrayList<>();
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                // RuntimeExceptionWithStack carries the lsf stack captured at
                // the originating action; surface that stack alongside the
                // wrapped cause. Anything else (e.g. TimeoutException from
                // notification retention sweep, or any unexpected error)
                // would otherwise be silently dropped — surface it with no
                // lsf stack rather than swallowing.
                if (cause instanceof RuntimeExceptionWithStack) {
                    throwables.add(new ThrowableWithStack(cause.getCause(), ((RuntimeExceptionWithStack) cause).lsfStack));
                } else if (cause != null) {
                    throwables.add(new ThrowableWithStack(cause));
                }
            }
        }
        return throwables.isEmpty() ? null : new NestedThreadException(throwables.toArray(new ThrowableWithStack[0]));
    }

    /**
     * Optional pre-step in {@link #await()} (e.g. server-pool's
     * executor.awaitTermination). Default: no-op, returns true.
     */
    protected boolean awaitReady() throws InterruptedException {
        return true;
    }

    public abstract void shutdown();

    public abstract void interruptIfPossible(ExecutionContext<?> context);
}
