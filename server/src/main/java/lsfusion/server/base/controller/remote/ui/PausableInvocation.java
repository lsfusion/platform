package lsfusion.server.base.controller.remote.ui;

import lsfusion.server.base.controller.remote.context.ContextAwarePendingRemoteObject;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.physics.admin.log.ServerLoggers;

import java.util.concurrent.*;

public abstract class PausableInvocation<T, E extends Exception> implements Callable<T> {

    protected final String sid;
    private final ExecutorService invocationsExecutor;

    private InvocationResult invocationResult;
    private Future<?> invocationFuture;
    
    //т.к. это просто вспомогательный объект, то достаточно одного статического
    private final Object syncMain = new Object();
    private final Object syncInvocation = new Object();

    /**
     * @param invocationsExecutor
     */
    public PausableInvocation(String sid, ExecutorService invocationsExecutor) {
        this.sid = sid;
        this.invocationsExecutor = invocationsExecutor;
    }

    public String getSID() {
        return sid;
    }

    // should be called from the remote call (main) thread
    public final T execute() throws E {
        invocationFuture = invocationsExecutor.submit(() -> {
            ServerLoggers.pausableLog("Run invocation: " + sid);

            InvocationResult result;
            try {
                runInvocation();
                result = InvocationResult.FINISHED;
                ServerLoggers.pausableLog("Invocation " + sid + " finished");
            } catch (Throwable t) {
                result = new InvocationResult(t);
                ServerLoggers.pausableLog("Invocation " + sid + " thrown an exception: ", t);
            }

            releaseMain(result);
        });

        return resume();
    }

    public final T call() throws E {
        return execute();
    }

    public final void cancel() {
        invocationFuture.cancel(true);
    }

    /**
     * рабочий поток
     */
    protected abstract void runInvocation() throws Throwable;

    /**
     * основной поток
     * @param t
     */
    protected abstract T handleThrows(ThrowableWithStack t) throws E;

    /**
     * основной поток
     */
    protected abstract T handleFinished() throws E;

    /**
     * основной поток
     */
    protected abstract T handlePaused() throws E;

    // should be called from the remote call (main) thread
    public final T resumeAfterPause() throws E {
        releaseInvocation();

        return resume();
    }

    // should be called from the remote call (main) thread
    public final T resume() throws E {
        InvocationResult result = blockMain();

        switch (result.getStatus()) {
            case PAUSED:
                return handlePaused();
            case EXCEPTION_THROWN:
                return handleThrows(result.getThrowable());
            case FINISHED:
                return handleFinished();
            default:
                throw new IllegalStateException("Shouldn't happen");
        }
    }

    // should be called from the worker (invocation) thread
    public final void pause() {
        releaseMain(InvocationResult.PAUSED);

        blockInvocation();
    }

    // should be called from the remote call (main) thread
    private InvocationResult blockMain() {
        ExecutionStackAspect.take(getRemoteObject(), () -> {
            synchronized (syncMain) {
                while(invocationResult == null) // releaseMain hasn't already been called
                    syncMain.wait();
            }
        });

        // should not be synchronized because it can be set to null, only in remote call (main) thread, i.e. this thread
        ServerLoggers.assertLog(invocationResult != null, "SHOULD HAVE INVOCATION RESULT");
        return invocationResult;
    }

    // should be called from the worker (invocation) thread
    private void releaseMain(InvocationResult result) {
        ServerLoggers.assertLog(result != null && invocationResult == null, "SHOULD HAVE NO INVOCATION RESULT");
        invocationResult = result;

        ExecutionStackAspect.take(getRemoteObject(), () -> {
            synchronized (syncMain) {
                syncMain.notifyAll();
            }
        });
    }

    // should be called from the worker (invocation) thread
    private void blockInvocation() {
        ExecutionStackAspect.take(getRemoteObject(), () -> {
            synchronized (syncInvocation) {
                while(invocationResult != null) { //releaseInvocation has not been called
                    ServerLoggers.assertLog(invocationResult == InvocationResult.PAUSED, "SHOULD BE PAUSED");

                    syncInvocation.wait();
                }
            }
        });
    }

    // should be called from the remote call (main) thread
    private void releaseInvocation() {
        ServerLoggers.assertLog(invocationResult == InvocationResult.PAUSED, "SHOULD BE PAUSED");
        invocationResult = null;

        ExecutionStackAspect.take(getRemoteObject(), () -> {
            synchronized (syncInvocation) {
                syncInvocation.notifyAll();
            }
        });
    }

    protected abstract ContextAwarePendingRemoteObject getRemoteObject();

}
