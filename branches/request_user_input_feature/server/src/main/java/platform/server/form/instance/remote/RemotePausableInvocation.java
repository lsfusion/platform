package platform.server.form.instance.remote;

import com.google.common.base.Preconditions;
import platform.base.ExceptionUtils;
import platform.interop.action.ClientAction;

import javax.mail.MethodNotSupportedException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;

public abstract class RemotePausableInvocation<T> extends PausableInvocation<T, RemoteException> {
    /**
     * @param invocationsExecutor
     */
    public RemotePausableInvocation(ExecutorService invocationsExecutor) {
        super(invocationsExecutor);
    }

    private T invocationResult = null;

    private ClientAction[] actions;
    private Object[] actionResults;

    /**
     * рабочий поток
     */
    public final Object[] pauseForUserInteraction(ClientAction... actions) {
        this.actions = actions;

        try {
            pause();
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was interrupted");
        }

        return actionResults;
    }

    /**
     * основной поток
     */
    public final T resumeAfterUserInteraction(Object[] actionResults) throws RemoteException {
        Preconditions.checkState(isPaused(), "can't resume after user interaction - wasn't paused for user interaction");

        this.actionResults = actionResults;
        return resume();
    }

    /**
     * <b>рабочий поток</b><br/>
     * по умолчанию вызывает {@link RemotePausableInvocation#callInvocation()}, и возвращает его результат из {@link RemotePausableInvocation#handleFinished()}
     * @throws Throwable
     */
    protected void runInvocation() throws Throwable {
        invocationResult = callInvocation();
    }

    /**
     * по умолчанию просто возвращает null,
     * переопредлять нужно либо данный метод, возвращая из него результат, либо {@link RemotePausableInvocation#runInvocation()}
     * вместе с {@link RemotePausableInvocation#handleFinished()}
     *
     * @return
     * @throws Throwable
     */
    protected T callInvocation() throws Throwable {
        return null;
    }

    /**
     * <b>основной поток</b><br/>
     * по умолчанию возвращает результат выполнения {@link RemotePausableInvocation#callInvocation()}
     * @return
     * @throws RemoteException
     */
    protected T handleFinished() throws RemoteException {
        return invocationResult;
    }

    /**
     * основной поток
     */
    protected T handleUserInteractionRequest(ClientAction... actions) throws Exception {
        throw new MethodNotSupportedException("User interation request isn't supported on this invocation");
    }

    @Override
    protected T handleThrows(Throwable t) throws RemoteException {
        throw ExceptionUtils.propogateRemoteException(t);
    }

    /**
     * основной поток
     */
    @Override
    protected final T handlePaused() throws RemoteException {
        try {
            return handleUserInteractionRequest(actions);
        } catch (Exception e) {
            throw ExceptionUtils.propogateRemoteException(e);
        }
    }
}
