package lsfusion.server.base.controller.remote;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.interop.action.*;
import lsfusion.interop.base.remote.RemoteRequestInterface;
import lsfusion.server.base.controller.remote.context.ContextAwarePendingRemoteObject;
import lsfusion.server.base.controller.remote.ui.RemotePausableInvocation;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.base.controller.thread.SyncType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.stack.EExecutionStackCallable;
import lsfusion.server.logics.action.controller.stack.EExecutionStackRunnable;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Optional.fromNullable;

public abstract class RemoteRequestObject extends ContextAwarePendingRemoteObject implements RemoteRequestInterface {

    protected final AtomicInteger numberOfFormChangesRequests = new AtomicInteger();
    private final SequentialRequestLock requestLock;
    private RemotePausableInvocation currentInvocation = null;

    private final Map<Long, Optional<?>> recentResults = Collections.synchronizedMap(new HashMap<>());
    private final Map<Long, Integer> requestsContinueIndices = Collections.synchronizedMap(new HashMap<>());

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

        if(requestIndex < minReceivedRequestIndex) // request can be lost and reach server only after retried and even next request already received, proceeded
            return null; // this check is important, because otherwise acquireRequestLock will never stop

        String invocationSID = generateInvocationSid(requestIndex);

        requestLock.acquireRequestLock(invocationSID, requestIndex);
        try {
            return callAndCacheResult(requestIndex, lastReceivedRequestIndex, () -> request.call(getStack()));
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

        if(requestIndex < minReceivedRequestIndex) // request can be lost and reach server only after retried and even next request already received, proceeded
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

        recentResults.put(requestIndex, fromNullable(result));

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
        if(lastReceivedRequestIndex == -1)
            return;
        // if request is already received, there is no need for recentResults for that request (we'll assume that there cannot be retryableRequest after that)
        // however it is < (and not <=) because the same requestIndex is used for continueServerInvocation (so it would be possible to clear result that might be needed for retryable request)
        // the cleaner solution is to lookahead if there will be continueServerInvocation and don't set lastReceivedRequestIndex in RmiQueue in that case, but now using < is a lot easier
        for (long i = minReceivedRequestIndex; i < lastReceivedRequestIndex; ++i) {
            recentResults.remove(i);
            requestsContinueIndices.remove(i);
        }
        minReceivedRequestIndex = lastReceivedRequestIndex;
    }

    protected abstract ServerResponse prepareResponse(long requestIndex, List<ClientAction> pendingActions, ExecutionStack stack, boolean forceLocalEvents);

    protected ServerResponse processPausableRMIRequest(final long requestIndex, long lastReceivedRequestIndex, final EExecutionStackRunnable runnable) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, runnable, false);
    }

    protected ServerResponse processPausableRMIRequest(final long requestIndex, long lastReceivedRequestIndex, final EExecutionStackRunnable runnable, boolean forceLocalEvents) throws RemoteException {

        return executeServerInvocation(requestIndex, lastReceivedRequestIndex, new RemotePausableInvocation(requestIndex, generateInvocationSid(requestIndex), pausablesExecutor, this) {
            @Override
            protected ServerResponse callInvocation() throws Throwable {
                ExecutionStack stack = getStack();
                runnable.run(stack);
                return prepareResponse(requestIndex, delayedActions, stack, forceLocalEvents);
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
            int i = 2;
            String methodName;
            while(true) {
                methodName = st[i].getMethodName();
                if(methodName.startsWith("process"))
                    i++;
                else
                    break;
            }

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
        return continueInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, () -> currentInvocation.resumeAfterUserInteraction(actionResults));
    }

    public ServerResponse throwInServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, final Throwable clientThrowable) throws RemoteException {
        return continueInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, () -> currentInvocation.resumeWithThrowable(clientThrowable));
    }

    public boolean isInServerInvocation(long requestIndex) throws RemoteException {
        boolean isInServerInvocation = currentInvocation != null;
        Object recentResult = recentResults.get(requestIndex).get();
        assert recentResult instanceof ServerResponse && isInServerInvocation == ((ServerResponse) recentResult).resumeInvocation;
        return isInServerInvocation;
    }

    private ServerResponse continueInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Callable<ServerResponse> continueRequest) throws RemoteException {
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

        assert currentInvocation.getRequestIndex() == requestIndex;

        requestsContinueIndices.put(requestIndex, continueIndex);

        return callAndCacheResult(requestIndex, lastReceivedRequestIndex, continueRequest);
    }

    public void delayUserInteraction(ClientAction action) {
        currentInvocation.delayUserInteraction(action);
    }

    public Object[] requestUserInteraction(ClientAction... actions) {
        return currentInvocation.pauseForUserInteraction(actions);
    }

    private Map<Long, SyncExecution> syncExecuteServerInvocationMap = MapFact.mAddRemoveMap();
    private Map<Long, SyncExecution> syncContinueServerInvocationMap = MapFact.mAddRemoveMap();
    private Map<Long, SyncExecution> syncThrowInServerInvocationMap = MapFact.mAddRemoveMap();
    private Map<Long, SyncExecution> syncProcessRMIRequestMap = MapFact.mAddRemoveMap();

    private static class SyncExecution {
        private boolean executed;
    }

    @Aspect
    public static class RemoteFormExecutionAspect {
        private Object syncExecute(Map<Long, SyncExecution> syncMap, long requestIndex, ProceedingJoinPoint joinPoint) throws Throwable {
            boolean needToWait = true;
            SyncExecution obj;
            Object result;

            synchronized (syncMap) {
                obj = syncMap.get(requestIndex);
                if (obj == null) { // this thread will do the calculation
                    obj = new SyncExecution();
                    syncMap.put(requestIndex, obj);
                    needToWait = false;
                }
            }

            if(needToWait) {
                synchronized (obj) {
                    while(!obj.executed)
                        obj.wait();
                }
            }

            try {
                result = joinPoint.proceed();
            } finally {
                if(!needToWait) {
                    synchronized (obj) {
                        obj.executed = true;
                        obj.notifyAll();
                    }
                    synchronized (syncMap) {
                        syncMap.remove(requestIndex);
                    }
                }
            }

            return result;
        }

        // syncing executions with the same index to avoid simultaneous executing retryable requests
        @Around("execution(* lsfusion.server.base.controller.remote.RemoteRequestObject.executeServerInvocation(long, long, lsfusion.server.base.controller.remote.ui.RemotePausableInvocation)) && target(object) && args(requestIndex, lastReceivedRequestIndex, invocation)")
        public Object execute(ProceedingJoinPoint joinPoint, RemoteRequestObject object, long requestIndex, long lastReceivedRequestIndex, RemotePausableInvocation invocation) throws Throwable {
            return syncExecute(object.syncExecuteServerInvocationMap, requestIndex, joinPoint);
        }

        @Around("execution(* lsfusion.server.base.controller.remote.RemoteRequestObject.continueServerInvocation(long, long, int, Object[])) && target(object) && args(requestIndex, lastReceivedRequestIndex, continueIndex, actionResults)")
        public Object execute(ProceedingJoinPoint joinPoint, RemoteRequestObject object, long requestIndex, long lastReceivedRequestIndex, int continueIndex, final Object[] actionResults) throws Throwable {
            return syncExecute(object.syncContinueServerInvocationMap, requestIndex, joinPoint);
        }
        @Around("execution(* lsfusion.server.base.controller.remote.RemoteRequestObject.throwInServerInvocation(long, long, int, Throwable)) && target(object) && args(requestIndex, lastReceivedRequestIndex, continueIndex, throwable)")
        public Object execute(ProceedingJoinPoint joinPoint, RemoteRequestObject object, long requestIndex, long lastReceivedRequestIndex, int continueIndex, Throwable throwable) throws Throwable {
            return syncExecute(object.syncThrowInServerInvocationMap, requestIndex, joinPoint);
        }

        @Around("execution(* lsfusion.server.base.controller.remote.RemoteRequestObject.processRMIRequest(long, long, lsfusion.server.logics.action.controller.stack.EExecutionStackCallable)) && target(object) && args(requestIndex, lastReceivedRequestIndex, request)")
        public Object execute(ProceedingJoinPoint joinPoint, RemoteRequestObject object, long requestIndex, long lastReceivedRequestIndex, EExecutionStackCallable request) throws Throwable {
            return syncExecute(object.syncProcessRMIRequestMap, requestIndex, joinPoint);
        }
    }
}