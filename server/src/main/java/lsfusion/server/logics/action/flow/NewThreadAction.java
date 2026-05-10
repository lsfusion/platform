package lsfusion.server.logics.action.flow;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.controller.stack.*;
import lsfusion.server.base.controller.thread.ExecutorFactory;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.implement.ActionMapImplement;
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

    private LP<?> targetProp;

    public <I extends PropertyInterface> NewThreadAction(LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                         ActionMapImplement<?, I> action,
                                                         PropertyInterfaceImplement<I> period,
                                                         PropertyInterfaceImplement<I> delay,
                                                         LP<?> targetProp) {
        super(caption, innerInterfaces, action);

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        if (period != null) {
            this.periodProp = period.map(mapInterfaces);
        }
        if (delay != null) {
            this.delayProp = delay.map(mapInterfaces);
        }
        this.targetProp = targetProp;
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
        // Single completion future per dispatch, owned by aroundAspect. It is
        // settled exactly once by whichever path terminates the work — server
        // worker via runWithCompletion, notification consumer via runWithCompletion,
        // sweep via Notification.onExpire, or the catch below if dispatch itself throws.
        // CompletableFuture's whenComplete fires once on the settling thread, so
        // unregisterThreadStack is invoked exactly once by construction — no
        // separate dispatched-flag and no per-path whenComplete needed.
        // Periodic dispatch is the deliberate exception: it never settles done,
        // so threadStack stays registered for the schedule lifetime (legacy).
        CompletableFuture<Void> done = new CompletableFuture<>();
        context.getSession().registerThreadStack();
        done.whenComplete((v, t) -> {
            try {
                context.getSession().unregisterThreadStack();
            } catch (SQLException ignored) {
            }
        });
        try {
            if (targetProp != null) {
                dispatchToTarget(context, done);
            } else {
                ScheduledFutureService scheduledService = context.getScheduledService();
                if (scheduledService instanceof ClientFutureService) {
                    if (delayProp != null || periodProp != null) {
                        throw new RuntimeException("SCHEDULE PERIOD/DELAY inside NEWEXECUTOR CLIENT is not supported yet");
                    }
                    dispatchToClient(context, (ClientFutureService) scheduledService, done);
                } else {
                    dispatchToServerPool(context, (ServerFutureService) scheduledService, done);
                }
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

    private void dispatchToTarget(ExecutionContext<PropertyInterface> context, CompletableFuture<Void> done) throws SQLException, SQLHandledException {
        dispatchAsNotification(context, done, (id, d) -> targetProp.change(id, context));
    }

    private void dispatchToClient(ExecutionContext<PropertyInterface> context, ClientFutureService scheduledService, CompletableFuture<Void> done) throws SQLException, SQLHandledException {
        dispatchAsNotification(context, done, (id, d) -> {
            scheduledService.getNavigatorsManager().deliverNotificationConnection(scheduledService.getClientConnection(), id);
            scheduledService.addFuture(d);
        });
    }

    /**
     * Pushes a notification (retention is taken from Settings inside the
     * Notification ctor), then runs branch-specific {@code postPush} (write
     * id to a TO property, deliver to a client). Three paths can settle
     * {@code done}: the consumer firing the notification via
     * {@code runNotification} → {@link #runWithCompletion}, the system
     * Scheduler sweep firing {@code onExpire}, or the caller's outer catch
     * on synchronous dispatch failure.
     */
    private void dispatchAsNotification(final ExecutionContext<PropertyInterface> context, final CompletableFuture<Void> done, IdHandler postPush) throws SQLException, SQLHandledException {
        postPush.handle(RemoteNavigator.pushGlobalNotification(new RemoteNavigator.Notification() {
            @Override
            public void run(ExecutionEnvironment env, ExecutionStack stack, PushAsyncResult asyncResult) {
                runWithCompletion(() -> NewThreadAction.this.run(context.override(env, stack, asyncResult)), done);
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
     * outer aroundAspect catch). Used by both client-mode notifications and
     * server-pool one-shot runnables.
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

    private void dispatchToServerPool(ExecutionContext<PropertyInterface> context, ServerFutureService scheduledService, CompletableFuture<Void> done) throws SQLException, SQLHandledException {
        long delay = delayProp != null ? ((Number) delayProp.read(context, context.getKeys())).longValue() : 0L;
        Long period = periodProp != null ? ((Number) periodProp.read(context, context.getKeys())).longValue() : null;

        ScheduledExecutorService executor = scheduledService != null
                ? scheduledService.getExecutor()
                : ExecutorFactory.createNewThreadService(context);

        if (period != null) {
            // periodic — fire-and-forget; done is intentionally never settled
            // so threadStack stays registered for the schedule lifetime
            // (legacy behavior).
            Runnable periodicRunnable = () -> run(context.override(ThreadLocalContext.getStack()));
            executor.scheduleAtFixedRate(periodicRunnable, delay, period, TimeUnit.MILLISECONDS);
            return;
        }

        executor.schedule(() -> runWithCompletion(
                () -> run(context.override(ThreadLocalContext.getStack())), done), delay, TimeUnit.MILLISECONDS);
        if (scheduledService != null) {
            scheduledService.addFuture(done);
        } else {
            // raw NEWTHREAD outside any NEWEXECUTOR: ad-hoc executor goes away
            // after the one-shot completes.
            executor.shutdown();
        }
    }

    @Override
    protected <T extends PropertyInterface> ActionMapImplement<?, PropertyInterface> createAspectImplement(ImSet<PropertyInterface> interfaces, ActionMapImplement<?, PropertyInterface> action) {
        return PropertyFact.createNewThreadAction(interfaces, action, periodProp, delayProp, targetProp);
    }

    @Override
    public boolean hasFlow(ChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if(!type.isSession())
            return false;
        return super.hasFlow(type, recursiveAbstracts);
    }
}
