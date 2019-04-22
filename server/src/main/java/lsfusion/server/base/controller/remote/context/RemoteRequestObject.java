package lsfusion.server.base.controller.remote.context;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import lsfusion.interop.action.*;
import lsfusion.server.base.controller.remote.SequentialRequestLock;
import lsfusion.server.base.controller.remote.ui.RemotePausableInvocation;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.base.controller.thread.SyncType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.stack.EExecutionStackCallable;
import lsfusion.server.logics.action.controller.stack.EExecutionStackRunnable;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.physics.admin.log.ServerLoggers;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Optional.fromNullable;

public abstract class RemoteRequestObject extends ContextAwarePendingRemoteObject {

    protected final AtomicInteger numberOfFormChangesRequests = new AtomicInteger();
    private final SequentialRequestLock requestLock;
    private RemotePausableInvocation currentInvocation = null;

    private final Map<Long, Optional<?>> recentResults = Collections.synchronizedMap(new HashMap<Long, Optional<?>>());
    private final Map<Long, Integer> requestsContinueIndices = Collections.synchronizedMap(new HashMap<Long, Integer>());

    private long minReceivedRequestIndex = 0;

    protected RemoteRequestObject(String sID) {
        super(sID);
        this.requestLock = new SequentialRequestLock();
    }

    protected RemoteRequestObject(int port, ExecutionStack upStack, String sID, SyncType type) throws RemoteException {
        super(port, upStack, sID, type);
        this.requestLock = new SequentialRequestLock();
    }

    protected <T> T processRMIRequest(long requestIndex, long lastReceivedRequestIndex, final EExecutionStackCallable<T> request) throws RemoteException {
        Optional<?> optionalResult = recentResults.get(requestIndex);
        if (optionalResult != null) {
            assert requestIndex >= minReceivedRequestIndex;
            return optionalResult(optionalResult);
        }

        if(requestIndex != -1 && requestIndex < minReceivedRequestIndex) // request can be lost and reach server only after retried and even next request already received, proceeded
            return null; // this check is important, because otherwise acquireRequestLock will never stop

        String invocationSID = generateInvocationSid(requestIndex);

        requestLock.acquireRequestLock(invocationSID, requestIndex);
        try {
            return callAndCacheResult(requestIndex, lastReceivedRequestIndex, new Callable<T>() {
                public T call() throws Exception {
                    return request.call(getStack());
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            requestLock.releaseRequestLock(invocationSID, requestIndex);
        }
    }

    protected ServerResponse executeServerInvocation(long requestIndex, long lastReceivedRequestIndex, RemotePausableInvocation invocation) throws RemoteException {
        Optional<?> optionalResult = recentResults.get(requestIndex);
        if (optionalResult != null) {
            ServerLoggers.pausableLog("Return cachedResult for: " + requestIndex);
            return optionalResult(optionalResult);
        }

        if(requestIndex != -1 && requestIndex < minReceivedRequestIndex) // request can be lost and reach server only after retried and even next request already received, proceeded
            return null; // this check is important, because otherwise acquireRequestLock will never stop and numberOfFormChangesRequests will be always > 0

        numberOfFormChangesRequests.incrementAndGet();
        requestLock.acquireRequestLock(invocation.getSID(), requestIndex);

        currentInvocation = invocation;

        return callAndCacheResult(requestIndex, lastReceivedRequestIndex, invocation);
    }

    protected <T> T callAndCacheResult(long requestIndex, long lastReceivedRequestIndex, Callable<T> request) throws RemoteException {
        clearRecentResults(lastReceivedRequestIndex);

        Object result;
        try {
            result = request.call();
        } catch (Throwable t) {
            result = new ThrowableWithStack(t);
        }

        if (requestIndex != -1) {
            recentResults.put(requestIndex, fromNullable(result));
        }

        return cachedResult(result);
    }

    protected <T> T optionalResult(Optional<?> optionalResult) throws RemoteException {
        if (!optionalResult.isPresent()) {
            return null;
        }

        return cachedResult(optionalResult.get());
    }

    /**
     * Если result instanceof Throwable, выбрасывает Exception, иначе кастит к T
     */
    private <T> T cachedResult(Object result) throws RemoteException {
        if (result instanceof ThrowableWithStack) {
            throw ((ThrowableWithStack) result).propagateRemote();
        } else {
            return (T) result;
        }
    }

    private void clearRecentResults(long lastReceivedRequestIndex) {
        //assert: current thread holds the request lock
        if (lastReceivedRequestIndex == -2) {
            recentResults.clear();
        } else {
            if(lastReceivedRequestIndex == -1)
                return;
            // if request is already received, there is no need for recentResults for that request (we'll assume that there cannot be retryableRequest after that)
            // however it is < (and not <=) because the same requestIndex is used for continueServerInvocation (so it would be possible to clear result that might be needed for retryable request)
            // the cleaner solution is to lookahead if there will be continueServerInvocation and don't set lastReceivedRequestIndex in RmiQueue in that case, but now using < is a lot easier
            for (long i = minReceivedRequestIndex; i < lastReceivedRequestIndex; ++i) {
                recentResults.remove(i);
            }
            minReceivedRequestIndex = lastReceivedRequestIndex;
        }
    }

    protected abstract ServerResponse prepareResponse(long requestIndex, List<ClientAction> pendingActions, boolean delayedGetRemoteChanges, boolean delayedHideForm, ExecutionStack stack);

    protected ServerResponse processPausableRMIRequest(final long requestIndex, long lastReceivedRequestIndex, final EExecutionStackRunnable runnable) throws RemoteException {

        return executeServerInvocation(requestIndex, lastReceivedRequestIndex, new RemotePausableInvocation(requestIndex, generateInvocationSid(requestIndex), pausablesExecutor, this) {
            @Override
            protected ServerResponse callInvocation() throws Throwable {
                ExecutionStack stack = getStack();
                runnable.run(stack);
                return prepareResponse(requestIndex, delayedActions, delayedGetRemoteChanges, delayedHideForm, stack);
            }

            @Override
            protected ServerResponse handleFinished() throws RemoteException {
                unlockNextRmiRequest();
                return super.handleFinished();
            }

            @Override
            protected ServerResponse handleThrows(ThrowableWithStack t) throws RemoteException {
                unlockNextRmiRequest();
                return super.handleThrows(t);
            }

            private void unlockNextRmiRequest() {
                currentInvocation = null;
                int left = numberOfFormChangesRequests.decrementAndGet();
                assert left >= 0;
                requestLock.releaseRequestLock(getSID(), requestIndex);
            }
        });
    }

    private String generateInvocationSid(long requestIndex) {
        String invocationSID;
        if (ServerLoggers.isPausableLogEnabled()) {
            StackTraceElement[] st = new Throwable().getStackTrace();
            String methodName = st[2].getMethodName();

            int aspectPostfixInd = methodName.indexOf("_aroundBody");
            if (aspectPostfixInd != -1) {
                methodName = methodName.substring(0, aspectPostfixInd);
            }

            invocationSID = "[f: " + getSID() + ", m: " + methodName + ", rq: " + requestIndex + "]";
        } else {
            invocationSID = "";
        }
        return invocationSID;
    }

    public ServerResponse continueServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, final Object[] actionResults) throws RemoteException {
        return continueInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, new Callable<ServerResponse>() {
            @Override
            public ServerResponse call() throws Exception {
                return currentInvocation.resumeAfterUserInteraction(actionResults);
            }
        });
    }

    public ServerResponse throwInServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, final Throwable clientThrowable) throws RemoteException {
        return continueInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, new Callable<ServerResponse>() {
            @Override
            public ServerResponse call() throws Exception {
                return currentInvocation.resumeWithThrowable(clientThrowable);
            }
        });
    }

    public boolean isInServerInvocation(long requestIndex) throws RemoteException {
        boolean isInServerInvocation = currentInvocation != null;
        Object recentResult = recentResults.get(requestIndex).get();
        assert recentResult instanceof ServerResponse && isInServerInvocation == ((ServerResponse) recentResult).resumeInvocation;
        return isInServerInvocation;
    }

    private ServerResponse continueInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Callable<ServerResponse> continueRequest) throws RemoteException {
        if (continueIndex != -1) {
            Integer cachedContinueIndex = requestsContinueIndices.get(requestIndex);
            if (cachedContinueIndex != null && cachedContinueIndex == continueIndex) {
                Optional<?> result = recentResults.get(requestIndex);
                ServerLoggers.pausableLog("Return cachedResult for continue: rq#" + requestIndex + "; cont#" + continueIndex);
                return optionalResult(result);
            }
            if (cachedContinueIndex == null) {
                cachedContinueIndex = -1;
            }

            //следующий continue может прийти только, если был получен предыдущий
            assert continueIndex == cachedContinueIndex + 1;
        }

        assert requestIndex == -1 || currentInvocation.getRequestIndex() == requestIndex;

        if (continueIndex != -1) {
            requestsContinueIndices.put(requestIndex, continueIndex);
        }

        return callAndCacheResult(requestIndex, lastReceivedRequestIndex, continueRequest);
    }

    public void delayUserInteraction(ClientAction action) {
        currentInvocation.delayUserInteraction(action);
    }

    public Object[] requestUserInteraction(ClientAction... actions) {
        return currentInvocation.pauseForUserInteraction(actions);
    }

    public void disconnect() throws SQLException, SQLHandledException {
        if (currentInvocation != null && currentInvocation.isPaused()) {
            try {
                currentInvocation.cancel();
            } catch (Exception e) {
                logger.warn("Exception was thrown, while invalidating form", e);
            }
        }
    }
}