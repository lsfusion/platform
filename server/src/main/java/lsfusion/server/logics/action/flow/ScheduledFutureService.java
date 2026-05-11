package lsfusion.server.logics.action.flow;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.base.controller.stack.NestedThreadException;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

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
 *
 * Also holds NEWTHREAD ... TO transit drained by NewExecutorAction after await().
 */
public abstract class ScheduledFutureService {
    protected final List<Future<?>> futures = Collections.synchronizedList(new ArrayList<>());

    private final List<StagedResult> stagedResults = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger dispatchCounter = new AtomicInteger();
    private final boolean awaited;

    protected ScheduledFutureService(boolean awaited) {
        this.awaited = awaited;
    }

    boolean isAwaited() {
        return awaited;
    }

    int nextDispatchCounter() {
        return dispatchCounter.getAndIncrement();
    }

    /** Stages a batch of (keyList → value) rows for a target. keyList already includes the
     *  optional counter prefix. Multiple stages for the same target are not aggregated —
     *  they're applied as separate batches in staging order. */
    void stageResults(LP<?> target, ImMap<ImList<DataObject>, Object> rows) {
        stagedResults.add(new StagedResult(target, rows));
    }

    void applyResults(ExecutionContext<?> context) throws SQLException, SQLHandledException {
        // Snapshot to release the list lock before running SQL writes.
        List<StagedResult> snapshot;
        synchronized (stagedResults) {
            snapshot = new ArrayList<>(stagedResults);
        }
        // Batch-write each staged result via changeList (single SQL temp table + PropertyChange apply).
        for (StagedResult sr : snapshot) {
            sr.target.changeList(context.getSession(), context.getEnv(),
                    sr.rows.mapKeys(keyList -> keyList.mapListValues((DataObject k) -> k.getValue())));
        }
    }

    private static class StagedResult {
        final LP<?> target;
        final ImMap<ImList<DataObject>, Object> rows;

        StagedResult(LP<?> target, ImMap<ImList<DataObject>, Object> rows) {
            this.target = target;
            this.rows = rows;
        }
    }

    public void addFuture(Future<?> future) {
        futures.add(future);
    }

    public NestedThreadException await(long timeoutMs) throws InterruptedException, TimeoutException {
        if (!awaitReady(timeoutMs)) {
            throw new TimeoutException();
        }
        List<ThrowableWithStack> throwables = new ArrayList<>();
        for (Future<?> future : futures) {
            try {
                future.get(timeoutMs, TimeUnit.MILLISECONDS);
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
     * Optional pre-step in {@link #await(long)} (e.g. server-pool's
     * executor.awaitTermination). Default: no-op, returns true. Returning {@code false} signals
     * the timeout elapsed during the pre-step → caller treats it as a timeout.
     */
    protected boolean awaitReady(long timeoutMs) throws InterruptedException {
        return true;
    }

    public abstract void shutdown();

    public abstract void interruptIfPossible(ExecutionContext<?> context);
}
