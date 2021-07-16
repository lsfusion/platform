package lsfusion.server.base.controller.remote.ui;

import com.google.common.base.Throwables;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.profiler.ExecutionTimeCounter;
import lsfusion.server.physics.admin.profiler.Profiler;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

public abstract class PausableInvocation<T, E extends Exception> implements Callable<T> {

    protected final String sid;
    private final ExecutorService invocationsExecutor;

    private InvocationResult invocationResult;
    private Future<?> invocationFuture;
    
    //т.к. это просто вспомогательный объект, то достаточно одного статического
    private final static Object syncObject = new Object();
    private final SynchronousQueue syncMain = new SynchronousQueue();
    private final SynchronousQueue syncInvocation = new SynchronousQueue();

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

    /**
     * Должно вызываться в основном потоке
     */
    public final T execute() throws E {
        invocationFuture = invocationsExecutor.submit(() -> {
            try {
                blockInvocation();
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
            
            ServerLoggers.pausableLog("Run invocation: " + sid);

            try {
                runInvocation();
                invocationResult = InvocationResult.FINISHED;
                ServerLoggers.pausableLog("Invocation " + sid + " finished");
            } catch (Throwable t) {
                invocationResult = new InvocationResult(t);
                ServerLoggers.pausableLog("Invocation " + sid + " thrown an exception: ", t);
            }

            try {
                releaseMain();
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
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

    /**
     * Должно вызываться в основном потоке <br/>
     */
    public final T resume() throws E {
        try {
            releaseInvocation();
            blockMain();
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }

        switch (invocationResult.getStatus()) {
            case PAUSED:
                return handlePaused();
            case EXCEPTION_THROWN:
                return handleThrows(invocationResult.getThrowable());
            case FINISHED:
                return handleFinished();
            default:
                throw new IllegalStateException("Shouldn't happen");
        }
    }

    /**
     * Должно вызываться в рабочем потоке. <br/>
     */
    public final void pause() throws InterruptedException {
        invocationResult = InvocationResult.PAUSED;

        releaseMain();
        blockInvocation();
    }

    public final boolean isPaused() {
        return invocationResult == InvocationResult.PAUSED;
    }

    private void blockMain() throws InterruptedException {
        blockSync(syncMain);
    }

    private void releaseMain() throws InterruptedException {
        releaseSync(syncMain);
    }

    private void blockInvocation() throws InterruptedException {
        blockSync(syncInvocation);
    }

    private void releaseInvocation() throws InterruptedException {
        releaseSync(syncInvocation);
    }
    
    protected abstract boolean isDeactivating();

    private void blockSync(SynchronousQueue sync) throws InterruptedException {
        long startTime = 0;
        ExecutionTimeCounter counter = ExecutionStackAspect.executionTime.get();
        if (counter != null) {
            startTime = System.nanoTime();
        }
        try {
            sync.take();
        } catch (InterruptedException e) {
            ServerLoggers.assertLog(isDeactivating(), "SHOULD NOT BE INTERRUPTED"); // не должен прерываться так как нарушит синхронизацию main - invocation
            throw e;
        }
        if (counter != null)
            counter.addUI(System.nanoTime() - startTime);
    }

    private void releaseSync(SynchronousQueue sync) throws InterruptedException {
        try {
            sync.put(syncObject); // тут по идее release должен сразу выйти и ничего не ждать (может быть проблема если interrupt'ся take, но непонятно что с этим в принципе делать)
        } catch (InterruptedException e) {
            ServerLoggers.assertLog(invocationsExecutor != null && invocationsExecutor.isShutdown(), "SHOULD NOT BE INTERRUPTED"); // shouldn't be interrupted because it will break main - invocation synchronization
            throw e;
        }
    }
}
