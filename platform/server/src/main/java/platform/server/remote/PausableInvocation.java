package platform.server.remote;

import org.apache.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

public abstract class PausableInvocation<T, E extends Exception> {
    protected final static Logger logger = Logger.getLogger(PausableInvocation.class);

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

    /**
     * Должно вызываться в основном потоке
     */
    public final T execute() throws E {
        invocationFuture = invocationsExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    blockInvocation();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                logger.debug("Run invocation: " + sid);

                try {
                    runInvocation();
                    invocationResult = InvocationResult.FINISHED;
                    logger.debug("Invocation " + sid + " finished");
                } catch (Throwable t) {
                    invocationResult = new InvocationResult(t);
                    logger.debug("Invocation " + sid + " thrown an exception: ", t);
                }

                try {
                    releaseMain();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        return resume();
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
     */
    protected abstract T handleThrows(Throwable t) throws E;

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
            throw new RuntimeException(e);
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

    private void blockSync(SynchronousQueue sync) throws InterruptedException {
        sync.take();
    }

    private void releaseSync(SynchronousQueue sync) throws InterruptedException {
        sync.put(syncObject);
    }
}
