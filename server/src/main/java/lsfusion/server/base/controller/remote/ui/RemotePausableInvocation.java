package lsfusion.server.base.controller.remote.ui;

import com.google.common.base.Throwables;
import lsfusion.base.ExceptionUtils;
import lsfusion.interop.action.*;
import lsfusion.server.base.controller.remote.context.ContextAwarePendingRemoteObject;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.physics.admin.log.ServerLoggers;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public abstract class RemotePausableInvocation extends PausableInvocation<ServerResponse, RemoteException> {
    private final ContextAwarePendingRemoteObject remoteObject;

    private final long requestIndex;

    public RemotePausableInvocation(long requestIndex, String sid, ExecutorService invocationsExecutor, ContextAwarePendingRemoteObject remoteObject) {
        super(sid, invocationsExecutor);
        this.requestIndex = requestIndex;
        this.remoteObject = remoteObject;
    }

    private ServerResponse invocationResult = null;

    protected List<ClientAction> delayedActions = new ArrayList<>();

    protected MessageClientAction delayedMessageAction = null;

    private Object[] actionResults;
    private Throwable clientThrowable;

    /**
     * рабочий поток
     */
    public final void delayUserInteraction(ClientAction action) {
        if (ServerLoggers.isPausableLogEnabled()) {
            ServerLoggers.pausableLog("Interaction " + sid + " called delayUserInteraction: " + action);
        }

        if (action instanceof MessageClientAction) {
            MessageClientAction messageAction = (MessageClientAction) action;
            if (delayedMessageAction == null || !isMergeable(delayedMessageAction, messageAction))
                delayedMessageAction = messageAction;
            else {
                delayedMessageAction.message += "\n" + messageAction.message;
                delayedMessageAction.textMessage += "\n" + messageAction.textMessage;
                return; // we've already added message to existing action
            }
        }

        delayedActions.add(action);
    }

    private boolean isMergeable(MessageClientAction action1, MessageClientAction action2) {
        return action1.type.equals(action2.type)
                && action1.data.isEmpty() && action2.data.isEmpty()
                && action1.titles.isEmpty() && action2.titles.isEmpty() && action1.message instanceof String && action2.message instanceof String;
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

        pause();

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
        if (ServerLoggers.isPausableLogEnabled())
            ServerLoggers.pausableLog("Interaction " + sid + " resumed after userInteraction: " + Arrays.toString(actionResults));

        this.actionResults = actionResults;
        return resumeAfterPause();
    }

    public final ServerResponse resumeWithThrowable(Throwable clientThrowable) throws RemoteException {
        if (ServerLoggers.isPausableLogEnabled())
            ServerLoggers.pausableLog("Interaction " + sid + " thrown client exception: ", clientThrowable);

        this.clientThrowable = clientThrowable;
        return resumeAfterPause();
    }

    /**
     * <b>рабочий поток</b><br/>
     * по умолчанию вызывает {@link RemotePausableInvocation#callInvocation()}, и возвращает его результат из {@link RemotePausableInvocation#handleFinished()}
     * @throws Throwable
     */

    private static final ThreadLocal<RemotePausableInvocation> runningInvocation = new ThreadLocal<>();
    protected void runInvocation() throws Throwable {
        //final long id = Thread.currentThread().getId();
        //RemoteLoggerAspect.putDateTimeCall(id, new Timestamp(System.currentTimeMillis()));
        //try {
            remoteObject.addContextThread(Thread.currentThread());
            runningInvocation.set(this);
            try {
                invocationResult = callInvocation();
            } finally {
                runningInvocation.set(null);
                remoteObject.removeContextThread(Thread.currentThread());
            }
        //} finally {
        //    RemoteLoggerAspect.removeDateTimeCall(id);
        //}
    }

    public static <T> T runUserInteraction(Function<RemotePausableInvocation, T> userInteraction) {
        return userInteraction.apply(runningInvocation.get());
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
    protected ServerResponse handleThrows(ThrowableWithStack t) throws RemoteException {
        throw t.propagateRemote();
    }

    /**
     * основной поток
     */
    @Override
    protected final ServerResponse handlePaused() throws RemoteException {
        try {
            ServerResponse result = new ServerResponse(requestIndex, delayedActions.toArray(new ClientAction[delayedActions.size()]), true);
            delayedActions.clear();
            delayedMessageAction = null;

            return result;
        } catch (Exception e) {
            throw ExceptionUtils.propagateRemoteException(e);
        }
    }

    public long getRequestIndex() {
        return requestIndex;
    }

    @Override
    protected ContextAwarePendingRemoteObject getRemoteObject() {
        return remoteObject;
    }
}
