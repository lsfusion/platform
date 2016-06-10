package lsfusion.server.remote;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import lsfusion.base.ExceptionUtils;
import lsfusion.interop.action.*;
import lsfusion.interop.form.ServerResponse;
import lsfusion.server.ServerLoggers;
import lsfusion.server.stack.ExecutionStackAspect;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

public abstract class RemotePausableInvocation extends PausableInvocation<ServerResponse, RemoteException> {
    private final ContextAwarePendingRemoteObject remoteObject;

    private final long requestIndex;

    /**
     * @param invocationsExecutor
     */
    public RemotePausableInvocation(ExecutorService invocationsExecutor, ContextAwarePendingRemoteObject remoteObject) {
        this(-1, null, invocationsExecutor, remoteObject);
    }

    public RemotePausableInvocation(long requestIndex, String sid, ExecutorService invocationsExecutor, ContextAwarePendingRemoteObject remoteObject) {
        super(sid, invocationsExecutor);
        this.requestIndex = requestIndex;
        this.remoteObject = remoteObject;
    }

    private ServerResponse invocationResult = null;

    protected List<ClientAction> delayedActions = new ArrayList<ClientAction>();

    protected MessageClientAction delayedMessageAction = null; 
    protected boolean delayedGetRemoteChanges = false;
    protected boolean delayedHideForm = false;

    private Object[] actionResults;
    private Throwable clientThrowable;

    public final String getLogMessage() {
        String result = "";
        for (ClientAction action : delayedActions) {
            if (action instanceof LogMessageClientAction) {
                result = (result.length() == 0 ? "" : result + '\n') + ((LogMessageClientAction) action).message;
            }
        }
        return result;
    } 
    
    /**
     * рабочий поток
     */
    public final void delayUserInteraction(ClientAction action) {
        if (ServerLoggers.isPausableLogEnabled()) {
            ServerLoggers.pausableLog("Interaction " + sid + " called delayUserInteraction: " + action);
        }

        if (action instanceof AsyncGetRemoteChangesClientAction) {
            if (!delayedGetRemoteChanges) {
                delayedActions.add(action);
                delayedGetRemoteChanges = true;
            }
        } else if (action instanceof HideFormClientAction) { // оптимизация, чтобы не делать round-trip в HideFormClientAction
            if (!delayedHideForm) {
                delayedActions.add(action);
                delayedHideForm = true;
            }
        } else if (action instanceof MessageClientAction) {
            if (delayedMessageAction == null) {
                delayedActions.add(action);
                delayedMessageAction = (MessageClientAction) action;
            } else {
                delayedMessageAction.message += "\n" + ((MessageClientAction) action).message;
            }
        } else {
            delayedActions.add(action);
        }
    }

    /**
     * рабочий поток
     */
    public final Object[] pauseForUserInteraction(ClientAction... actions) {
        if (ServerLoggers.isPausableLogEnabled()) {
            ServerLoggers.pausableLog("Interaction " + sid + " called pauseForUserInteraction: " + Arrays.toString(actions));
        }

        int neededActionResultsCnt = actions.length;
        Collections.addAll(delayedActions, actions);

        try {
            pause();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interaction " + sid + " was interrupted");
        }

        if (clientThrowable != null) {
            Throwable t = clientThrowable;
            clientThrowable = null;
            throw Throwables.propagate(t);
        }

        return Arrays.copyOfRange(actionResults, actionResults.length - neededActionResultsCnt, actionResults.length);
    }

    /**
     * основной поток
     */
    public final ServerResponse resumeAfterUserInteraction(Object[] actionResults) throws RemoteException {
        Preconditions.checkState(isPaused(), "can't resume after user interaction - wasn't paused for user interaction");

        if (ServerLoggers.isPausableLogEnabled()) {
            ServerLoggers.pausableLog("Interaction " + sid + " resumed after userInteraction: " + Arrays.toString(actionResults));
        }

        this.actionResults = actionResults;
        return resume();
    }

    public final ServerResponse resumeWithThrowable(Throwable clientThrowable) throws RemoteException {
        Preconditions.checkState(isPaused(), "can't resume after user interaction - wasn't paused for user interaction");

        ServerLoggers.pausableLog("Interaction " + sid + " thrown client exception: ", clientThrowable);

        this.clientThrowable = clientThrowable;
        return resume();
    }

    /**
     * <b>рабочий поток</b><br/>
     * по умолчанию вызывает {@link RemotePausableInvocation#callInvocation()}, и возвращает его результат из {@link RemotePausableInvocation#handleFinished()}
     * @throws Throwable
     */
    protected void runInvocation() throws Throwable {
        final long id = Thread.currentThread().getId();
        RemoteLoggerAspect.putDateTimeCall(id, new Timestamp(System.currentTimeMillis()));
        try {
            remoteObject.addLinkedThread(Thread.currentThread());
            try {
                invocationResult = callInvocation();
            } finally {
                remoteObject.removeLinkedThread(Thread.currentThread());
            }
        } finally {
            RemoteLoggerAspect.removeDateTimeCall(id);
        }
    }

    /**
     * по умолчанию просто возвращает null,
     * переопредлять нужно либо данный метод, возвращая из него результат, либо {@link PausableInvocation#runInvocation()}
     * вместе с {@link RemotePausableInvocation#handleFinished()}
     *
     * @return
     * @throws Throwable
     */
    protected ServerResponse callInvocation() throws Throwable {
        return null;
    }

    /**
     * <b>основной поток</b><br/>
     * по умолчанию возвращает результат выполнения {@link RemotePausableInvocation#callInvocation()}
     * @return
     * @throws RemoteException
     */
    protected ServerResponse handleFinished() throws RemoteException {
        return invocationResult;
    }

    @Override
    protected ServerResponse handleThrows(Throwable t) throws RemoteException {
        ExecutionStackAspect.setStackString(getLSFStack());
        throw ExceptionUtils.propagateRemoteException(t);
    }

    /**
     * основной поток
     */
    @Override
    protected final ServerResponse handlePaused() throws RemoteException {
        try {
            ServerResponse result = new ServerResponse(requestIndex, delayedActions.toArray(new ClientAction[delayedActions.size()]));
            delayedActions.clear();

            return result;
        } catch (Exception e) {
            throw ExceptionUtils.propagateRemoteException(e);
        }
    }

    public long getRequestIndex() {
        return requestIndex;
    }
}
