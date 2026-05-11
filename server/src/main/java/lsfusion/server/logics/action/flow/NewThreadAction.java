package lsfusion.server.logics.action.flow;

import com.google.common.base.Throwables;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.controller.stack.*;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.form.interactive.action.async.PushAsyncResult;
import lsfusion.server.logics.navigator.controller.remote.RemoteNavigator;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NewThreadAction extends AroundAspectAction {

    private PropertyInterfaceImplement<PropertyInterface> periodProp;
    private PropertyInterfaceImplement<PropertyInterface> delayProp;

    private final LP<?> notificationIdProp;
    private final ResultTarget resultTarget;

    public <I extends PropertyInterface> NewThreadAction(LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                         ActionMapImplement<?, I> action,
                                                         PropertyInterfaceImplement<I> period,
                                                         PropertyInterfaceImplement<I> delay,
                                                         LP<?> notificationIdProp,
                                                         ResultTarget resultTarget) {
        super(caption, innerInterfaces, action);

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        if (period != null) {
            this.periodProp = period.map(mapInterfaces);
        }
        if (delay != null) {
            this.delayProp = delay.map(mapInterfaces);
        }
        this.notificationIdProp = notificationIdProp;
        this.resultTarget = resultTarget;
    }

    ResultTarget getResultTarget() {
        return resultTarget;
    }

    // in theory we can also pass Thread, and then add ExecutionStackAspect.getStackString to message (to get multi thread stack)
    @StackNewThread
    @StackMessage("NEWTHREAD")
    @ThisMessage
    protected void run(ExecutionContext<PropertyInterface> context) { //, @ParamMessage (profile = false) String callThreadStack) {
        try {
            proceed(context);
        } catch (Throwable t) {
            ServerLoggers.schedulerLogger.error("New thread error : ", t);
            throw new RuntimeExceptionWithStack(t);
        }
    }

    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        // TO requires an awaited scope; validated before registerThreadStack so an early
        // throw doesn't leak the registration.
        if (resultTarget != null) {
            ScheduledFutureService outer = context.getScheduledService();
            if (outer == null || !outer.isAwaited()) {
                throw new RuntimeException("NEWTHREAD ... TO requires an enclosing NEWEXECUTOR ... WAIT scope");
            }
        }
        // Read SCHEDULE values once — a NULL-valued PERIOD prop is treated as "no periodic"
        // (one-shot dispatch); NULL-valued DELAY is treated as 0. Same source of truth for the
        // NOWAIT check and post-dispatch done settling.
        Number delayValue = delayProp != null ? (Number) delayProp.read(context, context.getKeys()) : null;
        long delay = delayValue != null ? delayValue.longValue() : 0L;
        Number periodValue = periodProp != null ? (Number) periodProp.read(context, context.getKeys()) : null;
        Long period = periodValue != null ? periodValue.longValue() : null;
        if (delay < 0) {
            throw new RuntimeException("NEWTHREAD ... SCHEDULE DELAY must be >= 0, got " + delay);
        }
        if (period != null && period <= 0) {
            throw new RuntimeException("NEWTHREAD ... SCHEDULE PERIOD must be > 0, got " + period);
        }
        // PERIOD is fire-and-forget by design — require NOWAIT so we don't have to track the
        // schedule's lifetime for a hypothetical WAIT joiner that would never see it complete.
        if (period != null) {
            ScheduledFutureService outer = context.getScheduledService();
            if (outer != null && outer.isAwaited()) {
                throw new RuntimeException("NEWTHREAD ... SCHEDULE PERIOD requires NEWEXECUTOR ... NOWAIT");
            }
        }

        // Single completion future per dispatch, owned by aroundAspect. Settled by whichever
        // path terminates the work — server worker / notification consumer via runWithCompletion,
        // sweep via Notification.onExpire, or the catch below on synchronous dispatch failure.
        // For periodic we settle done right after a successful dispatch — register/unregister
        // cancel out and the threadStack is not held across the (open-ended) schedule.
        CompletableFuture<Void> done = new CompletableFuture<>();
        context.getSession().registerThreadStack();
        done.whenComplete((v, t) -> {
            try {
                context.getSession().unregisterThreadStack();
            } catch (SQLException ignored) {
            }
        });
        // Counter is consumed at dispatch time (here, before any pool/notification work) so
        // slot order matches source order of NEWTHREAD calls, not completion order.
        final int counter = nextDispatchCounterIfNeeded(context.getScheduledService());
        try {
            if (notificationIdProp != null) {
                dispatchToNotification(context, done, counter);
            } else {
                ScheduledFutureService scheduledService = context.getScheduledService();
                if (scheduledService instanceof ClientFutureService) {
                    dispatchToClient(context, (ClientFutureService) scheduledService, done, counter, delay, period);
                } else {
                    dispatchToServerPool(context, (ServerFutureService) scheduledService, done, counter, delay, period);
                }
            }
            if (period != null) {
                // Periodic — schedule is armed; release threadStack now (no joiner, NOWAIT).
                done.complete(null);
            }
            return FlowResult.FINISH;
        } catch (Throwable t) {
            // Synchronous dispatch failure: settle done so the single sink
            // releases threadStack, then propagate. If a notification was
            // already pushed before the failure, sweep eventually expires it
            // and tries to settle done — no-op (already settled).
            done.completeExceptionally(t);
            throw t;
        }
    }

    private void dispatchToNotification(ExecutionContext<PropertyInterface> context, CompletableFuture<Void> done, int counter) throws SQLException, SQLHandledException {
        // CLIENT p alone is fire-and-forget — id goes to p and the developer triggers later.
        // CLIENT p TO q registers the future so an enclosing WAIT actually waits for that
        // external trigger and TO's RETURN can be captured and applied.
        final ScheduledFutureService scheduledService = context.getScheduledService();
        dispatchAsNotification(context, done, scheduledService, counter, false, 0L, (id, d) -> {
            notificationIdProp.change(id, context);
            if (resultTarget != null && scheduledService != null) {
                scheduledService.addFuture(d);
            }
        });
    }

    private void dispatchToClient(ExecutionContext<PropertyInterface> context, ClientFutureService scheduledService, CompletableFuture<Void> done, int counter, long delay, Long period) throws SQLException, SQLHandledException {
        boolean periodic = period != null;
        // Lease covers expected client cadence: one-shot uses delay; periodic uses
        // max(delay, period) — so a DELAY 10min + PERIOD 1s schedule isn't swept before the first
        // client fire at delay (and subsequent fires every period thereafter refresh the lease).
        // Once the client stops firing, the entry expires within retention + extraRetentionMs of
        // the last lease refresh and the sweep evicts it.
        long extraRetentionMs = periodic ? Math.max(delay, period) : delay;
        dispatchAsNotification(context, done, scheduledService, counter, periodic, extraRetentionMs, (id, d) -> {
            // Client-side scheduling — server passes delay/period along with the id; the client's
            // own scheduler fires the notification after delay (and re-fires every period for
            // periodic). Periodic is required to be NOWAIT (validated above), so no future
            // tracking on the service — only one-shot notifications join the WAIT awaitable.
            scheduledService.getNavigatorsManager().deliverNotificationConnection(scheduledService.getClientConnection(), id, delay, period);
            if (!periodic) {
                scheduledService.addFuture(d);
            }
        });
    }

    /**
     * Pushes a notification (retention taken from Settings inside the {@link RemoteNavigator.Notification}
     * ctor; {@code extraRetentionMs} extends it to cover expected client cadence), then runs
     * branch-specific {@code postPush} (write id to a CLIENT property, deliver to a client).
     * For one-shot dispatches, {@code done} is settled by whichever path terminates the work —
     * the consumer firing via {@code runNotification} → {@link #runWithCompletion}, the system
     * sweep firing {@code onExpire}, or the caller's outer catch on synchronous dispatch failure.
     * For {@code periodic} dispatches {@code done} is settled by aroundAspect right after a
     * successful dispatch (PERIOD requires NOWAIT, no joiner waits on it); the entry stays in
     * the global map across fires and the {@link RemoteNavigator.Notification#refreshLease()}
     * sliding lease drives map cleanup when the client stops firing.
     */
    private void dispatchAsNotification(final ExecutionContext<PropertyInterface> context, final CompletableFuture<Void> done, final ScheduledFutureService scheduledService, final int counter, final boolean periodic, long extraRetentionMs, IdHandler postPush) throws SQLException, SQLHandledException {
        postPush.handle(RemoteNavigator.pushGlobalNotification(new RemoteNavigator.Notification(periodic, extraRetentionMs) {
            @Override
            public void run(ExecutionEnvironment env, ExecutionStack stack, PushAsyncResult asyncResult) {
                ExecutionContext<PropertyInterface> runContext = context.override(env, stack, asyncResult);
                if (periodic) {
                    // Periodic — fire-and-forget; PERIOD requires NOWAIT so no result capture path exists.
                    NewThreadAction.this.run(runContext);
                } else {
                    runWithCompletion(() -> runAndCapture(runContext, scheduledService, counter), done);
                }
            }

            @Override
            protected Action<?> getAction() {
                return aspectActionImplement.action;
            }

            @Override
            protected void onExpire() {
                done.completeExceptionally(new TimeoutException(
                        "notification expired (no delivery within retention)"));
            }
        }), done);
    }

    @FunctionalInterface
    private interface IdHandler {
        void handle(int notificationId, CompletableFuture<Void> done) throws SQLException, SQLHandledException;
    }

    /**
     * Runs {@code action} and signals {@code done} accordingly. Skips the body if
     * {@code done} has already been settled (by retention timeout or by the
     * outer aroundAspect catch). Used by one-shot dispatches only — periodic dispatches run
     * the inner action bare (done is settled by aroundAspect right after a successful dispatch,
     * so the {@code isDone()} short-circuit would silently swallow every fire).
     */
    private static void runWithCompletion(Runnable action, CompletableFuture<Void> done) {
        if (done.isDone()) {
            return;
        }
        Throwable thrown = null;
        try {
            action.run();
        } catch (Throwable t) {
            thrown = t;
            throw t;
        } finally {
            if (thrown == null) done.complete(null);
            else done.completeExceptionally(thrown);
        }
    }

    private void dispatchToServerPool(ExecutionContext<PropertyInterface> context, ServerFutureService scheduledService, CompletableFuture<Void> done, int counter, long delay, Long period) throws SQLException, SQLHandledException {
        ScheduledExecutorService executor = scheduledService != null
                ? scheduledService.getExecutor()
                : ExecutorFactory.createNewThreadService(context);

        if (period != null) {
            // Periodic — fire-and-forget; PERIOD requires NOWAIT (so no result capture path).
            executor.scheduleAtFixedRate(
                    () -> run(context.override(ThreadLocalContext.getStack())),
                    delay, period, TimeUnit.MILLISECONDS);
            return;
        }

        executor.schedule(() -> runWithCompletion(
                () -> runAndCapture(context.override(ThreadLocalContext.getStack()), scheduledService, counter),
                done), delay, TimeUnit.MILLISECONDS);
        if (scheduledService != null) {
            scheduledService.addFuture(done);
        } else {
            // raw NEWTHREAD outside any NEWEXECUTOR: ad-hoc executor goes away
            // after the one-shot completes.
            executor.shutdown();
        }
    }

    private void runAndCapture(ExecutionContext<PropertyInterface> innerContext, ScheduledFutureService service, int counter) {
        run(innerContext);
        captureResult(innerContext, service, counter);
    }

    private int nextDispatchCounterIfNeeded(ScheduledFutureService service) {
        return (resultTarget != null && resultTarget.prependDispatchCounter && service != null)
                ? service.nextDispatchCounter() : 0;
    }

    private void captureResult(ExecutionContext<PropertyInterface> innerContext, ScheduledFutureService service, int counter) {
        if (resultTarget == null || service == null)
            return;
        try {
            DataObject ck = resultTarget.prependDispatchCounter ? counterKey(counter) : null;
            ImMap<ImList<DataObject>, Object> staged;
            if (resultTarget.returnLP.listInterfaces.size() == 0) {
                // Scalar — read() so `RETURN NULL` (and unwritten) explicitly stages a null write.
                // readAllClasses() would filter the null row via expr.getWhere() and silently
                // leave the outer target unchanged, which diverges from EXEC ... TO semantics.
                staged = MapFact.singleton(
                        ck != null ? ListFact.singleton(ck) : ListFact.<DataObject>EMPTY(),
                        resultTarget.returnLP.read(innerContext));
            } else {
                // Multi-arity — readAllClasses returns only the rows the inner action wrote;
                // unwritten/null rows leave outer target unchanged for those keys.
                staged = resultTarget.returnLP.readAllClasses(innerContext.getEnv()).mapKeyValues(
                        ck != null ? introKeys -> ListFact.<DataObject>singleton(ck).addList(introKeys) : introKeys -> introKeys,
                        (DataObject v) -> v.getValue());
            }
            if (!staged.isEmpty()) {
                service.stageResults(resultTarget.toProp, staged);
            }
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    private static DataObject counterKey(int counter) {
        return new DataObject(counter, IntegerClass.instance);
    }

    @Override
    protected <T extends PropertyInterface> ActionMapImplement<?, PropertyInterface> createAspectImplement(ImSet<PropertyInterface> interfaces, ActionMapImplement<?, PropertyInterface> action) {
        return PropertyFact.createNewThreadAction(interfaces, action, periodProp, delayProp, notificationIdProp, resultTarget);
    }

    @Override
    public boolean hasFlow(ChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if(!type.isSession())
            return false;
        return super.hasFlow(type, recursiveAbstracts);
    }

    /** TO clause descriptor. Scalar (no counter, 0-arity returnLP) writes are last-write-wins
     *  by completion order on the inner thread, not by source order of the NEWTHREAD calls. */
    public static final class ResultTarget {
        public final LP<?> toProp;
        public final LP<?> returnLP;
        public final boolean prependDispatchCounter;

        public ResultTarget(LP<?> toProp, LP<?> returnLP, boolean prependDispatchCounter) {
            this.toProp = toProp;
            this.returnLP = returnLP;
            this.prependDispatchCounter = prependDispatchCounter;
        }
    }
}
