package platform.server.form.instance.remote;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;

public class PausableInvocation<T, E extends Exception> {
    private final Runnable invocation;
    private final ExecutorService invocationsExecutor;

    private InvocationHandler<T, E> currentInvocationHandler;
    private InvocationResult invocationResult;
    private InvocationHandler<T, E> invocationResultHandler;

    //т.к. это просто вспомогательный объект, то достаточно одного статического
    private final static Object syncObject = new Object();
    private final SynchronousQueue syncMain = new SynchronousQueue();
    private final SynchronousQueue syncInvocation = new SynchronousQueue();

    /**
     * @param context               контекст
     * @param invocation              для запуска в рабочем потоке
     * @param invocationHandler для получения результата в основном потоке
     * @param invocationsExecutor
     */
    public PausableInvocation(ExecutorService invocationsExecutor, Runnable invocation, InvocationHandler<T, E> invocationHandler) {
        this.invocationsExecutor = invocationsExecutor;
        this.invocation = invocation;
        this.currentInvocationHandler = invocationHandler;
    }

    /**
     * Должно вызываться в основном потоке
     */
    public T execute() throws E {
        invocationsExecutor.execute(new Runnable() {
            @Override
            public void run() {
                blockInvocation();

                try {
                    invocation.run();
                    invocationResult = InvocationResult.FINISHED;
                } catch (Throwable t) {
                    invocationResult = new InvocationResult(t);
                }

                releaseMain();
            }
        });

        return resume();
    }

    /**
     * Должно вызываться в основном потоке <br/>
     * для получения результата будет вызываться тот же handler, что и для конечно результата
     */
    public T resume() throws E {
        return resume(currentInvocationHandler);
    }

    /**
     * Должно вызываться в основном потоке
     * @param invocationHandler будет использоваться для получения результата
     */
    public T resume(InvocationHandler<T, E> invocationHandler) throws E {
        this.currentInvocationHandler = invocationHandler;
        this.invocationResultHandler = invocationHandler;

        releaseInvocation();
        blockMain();

        return invocationResultHandler.handle(invocationResult);
    }

    /**
     * Должно вызываться в рабочем потоке. <br/>
     * для получения результата будет вызываться тот же handler, что и для конечно результата
     */
    public void pause() {
        pause(currentInvocationHandler);
    }

    /**
     * Должно вызываться в рабочем потоке. <br/>
     * @param result вернётся в качестве результата в основной поток
     */
    public void pause(final T result) {
        pause(new InvocationHandler<T, E>() {
            @Override
            public T handle(InvocationResult invocationResult) {
                return result;
            }
        });
    }

    /**
     * Должно вызываться в рабочем потоке. <br/>
     * @param invocationHandler будет использоваться для получения результата
     */
    public void pause(InvocationHandler<T, E> invocationHandler) {
        invocationResult = InvocationResult.PAUSED;
        invocationResultHandler = invocationHandler;

        releaseMain();
        blockInvocation();
    }

    private void blockMain() {
        blockSync(syncMain);
    }

    private void releaseMain() {
        releaseSync(syncMain);
    }

    private void blockInvocation() {
        blockSync(syncInvocation);
    }

    private void releaseInvocation() {
        releaseSync(syncInvocation);
    }

    private void blockSync(SynchronousQueue sync) {
        try {
            sync.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void releaseSync(SynchronousQueue sync) {
        try {
            sync.put(syncObject);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
