package lsfusion.server.base.controller.remote;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.lambda.EFunction;
import lsfusion.interop.action.*;
import lsfusion.interop.base.remote.RemoteRequestInterface;
import lsfusion.server.base.controller.context.Context;
import lsfusion.server.base.controller.remote.context.ContextAwarePendingRemoteObject;
import lsfusion.server.base.controller.remote.ui.RemotePausableInvocation;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.base.controller.thread.SyncType;
import lsfusion.server.logics.action.controller.stack.EExecutionStackCallable;
import lsfusion.server.logics.action.controller.stack.EExecutionStackRunnable;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.form.interactive.changed.FormChanges;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.physics.dev.id.name.CompoundNameUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.google.common.base.Optional.fromNullable;

public abstract class RemoteRequestObject extends ContextAwarePendingRemoteObject implements RemoteRequestInterface {

    private final SequentialRequestLock requestLock;

    protected Integer getInvocationsCount() {
        return currentInvocations.size();
    }

    private final Map<Long, RemotePausableInvocation> currentInvocations = Collections.synchronizedMap(new HashMap<>());

    private RemotePausableInvocation getInvocation(long requestIndex) {
        return currentInvocations.get(requestIndex);
    }

    private void startInvocation(long requestIndex, RemotePausableInvocation invocation) {
        currentInvocations.put(requestIndex, invocation);

        if(requestLock != null)
            requestLock.blockRequestLock(invocation.getSID(), requestIndex, this);
    }
    private void finishInvocation(long requestIndex, RemotePausableInvocation invocation) {
        currentInvocations.remove(requestIndex);
        if (requestLock == null) {
            // in unsynchronized mode clearRecentResults skips ongoing invocations' entries; clean them up now
            if (requestIndex < minReceivedRequestIndex) {
                recentResults.remove(requestIndex);
                requestsContinueIndices.remove(requestIndex);
            }
        } else
            requestLock.releaseRequestLock(invocation.getSID(), requestIndex, this);
    }

    private final Map<Long, Optional<?>> recentResults = Collections.synchronizedMap(new HashMap<>());
    private final Map<Long, Integer> requestsContinueIndices = Collections.synchronizedMap(new HashMap<>());

    private long minReceivedRequestIndex = 0;

    protected RemoteRequestObject(int port, ExecutionStack upStack, String sID, SyncType type) throws RemoteException {
        super(port, upStack, sID, type);

        setContext(createContext());

        this.requestLock = synchronizeRequests() ? new SequentialRequestLock() : null;
    }

    protected abstract Context createContext();

    protected boolean synchronizeRequests() { // should be synchronized with the same method / field in RemoteDispatchAsync, RmiQueue
        return true;
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

        if(requestLock != null)
            requestLock.blockRequestLock(invocationSID, requestIndex, this);
        try {
            return callAndCacheResult(requestIndex, lastReceivedRequestIndex, () -> request.call(getStack()));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            if(requestLock != null)
                requestLock.releaseRequestLock(invocationSID, requestIndex, this);
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

        startInvocation(requestIndex, invocation);

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

    // here there is a problem: this minReceivedRequestIndex should be used only when requests are synchronized
    // however it's not clear what to do in the opposite case (because in that, case requests are not synchronized on the client, so we should send all request indexes to understand which one we can dispose)
    // as a temporary solution, something like simple timer logics can be used (i.e dispose sent requests after some timeout), but for now we'll leave it this way
    private void clearRecentResults(long lastReceivedRequestIndex) {
        //assert: current thread holds the request lock
        if(lastReceivedRequestIndex == -1)
            return;
        // if request is already received, there is no need for recentResults for that request (we'll assume that there cannot be retryableRequest after that)
        // however it is < (and not <=) because the same requestIndex is used for continueServerInvocation (so it would be possible to clear result that might be needed for retryable request)
        // the cleaner solution is to lookahead if there will be continueServerInvocation and don't set lastReceivedRequestIndex in RmiQueue in that case, but now using < is a lot easier
        for (long i = minReceivedRequestIndex; i < lastReceivedRequestIndex; ++i) {
            // in unsynchronized mode lastReceivedRequestIndex can advance past ongoing paused invocations;
            // preserve their recentResults and requestsContinueIndices entries until they complete (see finishInvocation)
            if (requestLock != null || !currentInvocations.containsKey(i)) {
                recentResults.remove(i);
                requestsContinueIndices.remove(i);
            }
        }
        minReceivedRequestIndex = lastReceivedRequestIndex;
    }

    public void waitRecentResults(long waitRequestIndex) {
        try {
            if(waitRequestIndex != -1) { //see FormsController.executeNotificationAction
                while (!recentResults.containsKey(waitRequestIndex)) {
                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    protected abstract ServerResponse prepareResponse(long requestIndex, List<ClientAction> pendingActions, ExecutionStack stack, boolean forceLocalEvents, boolean paused);

    protected ServerResponse processPausableRMIRequest(final long requestIndex, long lastReceivedRequestIndex, final EExecutionStackRunnable runnable) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, runnable, false);
    }

    protected ServerResponse processPausableRMIRequest(final long requestIndex, long lastReceivedRequestIndex, final EExecutionStackRunnable runnable, boolean forceLocalEvents) throws RemoteException {

        return executeServerInvocation(requestIndex, lastReceivedRequestIndex, new RemotePausableInvocation(requestIndex, generateInvocationSid(requestIndex), pausablesExecutor, this) {
            @Override
            protected ServerResponse callInvocation() throws Throwable {
                ExecutionStack stack = getStack();
                runnable.run(stack);
                return prepareResponse(requestIndex, delayedActions, stack, forceLocalEvents, false);
            }

            @Override
            protected ServerResponse handlePaused() {
                delayedMessageAction = null;
                return prepareResponse(requestIndex, delayedActions, null, false, true);
            }

            @Override
            protected ServerResponse handleFinished() {
                unlockNextRmiRequest();
                return super.handleFinished();
            }

            @Override
            protected ServerResponse handleThrows(ThrowableWithStack t) throws RemoteException {
                unlockNextRmiRequest();
                return super.handleThrows(t);
            }

            private void unlockNextRmiRequest() {
                finishInvocation(requestIndex, this);
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

    public ServerResponse continueServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, final Object actionResult) throws RemoteException {
        return continueInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, currentInvocation -> currentInvocation.resumeAfterUserInteraction(actionResult));
    }

    public ServerResponse throwInServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, final Throwable clientThrowable) throws RemoteException {
        return continueInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, currentInvocation -> currentInvocation.resumeWithThrowable(clientThrowable));
    }

    public boolean isInServerInvocation(long requestIndex) throws RemoteException {
        boolean isInServerInvocation = getInvocation(requestIndex) != null;
        Object recentResult = recentResults.get(requestIndex).get();
        assert recentResult instanceof ServerResponse && isInServerInvocation == ((ServerResponse) recentResult).resumeInvocation;
        return isInServerInvocation;
    }

    private ServerResponse continueInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, EFunction<RemotePausableInvocation, ServerResponse, RemoteException> continueRequest) throws RemoteException {
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

        requestsContinueIndices.put(requestIndex, continueIndex);

        return callAndCacheResult(requestIndex, lastReceivedRequestIndex, () -> continueRequest.apply(getInvocation(requestIndex)));
    }

    public void delayUserInteraction(ClientAction action) {
        RemotePausableInvocation.runUserInteraction(currentInvocation -> { currentInvocation.delayUserInteraction(action); return null;});
    }

    public Object requestUserInteraction(ClientAction action) {
        return RemotePausableInvocation.runUserInteraction(currentInvocation -> currentInvocation.pauseForUserInteraction(action));
    }

    // ---- form/navigator JS controller: exec(action) / eval(script) / change(property) over the pausable channel ----
    // Shared orchestration (resolve by name -> bind params -> run -> read & serialize result -> deliver to the JS
    // callback). The execution context differs by remote and is supplied via the 3 hooks below: form runs in its
    // PERSISTENT session with a FormInstanceContext; navigator opens a FRESH session per call with the connection
    // context. See GFORM-CONTROLLER-EXEC-EVAL-PLAN.

    public ServerResponse exec(long requestIndex, long lastReceivedRequestIndex, long callbackId, String action, Object[] params) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            ClientAction terminal;
            try {
                LA<?> la = resolveAction(action);
                if(la == null)
                    throw new RuntimeException("Action was not found: " + action);
                controllerGate(la.getActionOrProperty().hasAnnotation("api")); // resolve -> gate(@api)
                terminal = runControllerAction(callbackId, la, params, stack);
            } catch (Exception e) { // business/property/parse/gate -> onException; non-Exception throwables propagate to the normal request-failure path
                terminal = new ControllerExceptionClientAction(callbackId, controllerMessage(e), false);
            }
            delayUserInteraction(terminal);
        });
    }

    public ServerResponse eval(long requestIndex, long lastReceivedRequestIndex, long callbackId, String script, boolean evalAction, Object[] params) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            ClientAction terminal;
            try {
                controllerGate(false); // gate BEFORE evaluateRun, no @api for a dynamic script
                LA<?> la = getControllerBL().evaluateRun(script, evalAction); // evalAction: true -> wrap body into run(); false -> script defines its own run (typed params)
                if(la == null)
                    throw new RuntimeException("Eval 'run' action was not found");
                terminal = runControllerAction(callbackId, la, params, stack);
            } catch (Exception e) {
                terminal = new ControllerExceptionClientAction(callbackId, controllerMessage(e), false);
            }
            delayUserInteraction(terminal);
        });
    }

    public ServerResponse change(long requestIndex, long lastReceivedRequestIndex, long callbackId, String property, Object[] params, Object value) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            ClientAction terminal;
            try {
                LP<?> lp = resolveProperty(property);
                if(lp == null)
                    throw new RuntimeException("Property was not found: " + property);
                controllerGate(lp.getActionOrProperty().hasAnnotation("api"));
                terminal = runControllerChange(callbackId, lp, params, value, stack);
            } catch (Exception e) {
                terminal = new ControllerExceptionClientAction(callbackId, controllerMessage(e), false);
            }
            delayUserInteraction(terminal);
        });
    }

    // hooks: form -> persistent form.session + FormInstanceContext; navigator -> fresh createSession() + connection context
    protected abstract BusinessLogics getControllerBL();
    protected abstract void controllerGate(boolean apiAnnotation);
    protected abstract ClientAction runControllerRequest(ExecutionStack stack, ControllerBody body) throws SQLException, SQLHandledException, ParseException, IOException;

    protected interface ControllerBody {
        ClientAction run(ExecutionEnvironment env, ConnectionContext context) throws SQLException, SQLHandledException, ParseException, IOException;
    }

    // resolve by compound name, falling back to external id (mirrors RemoteConnection.findAction)
    private LA<?> resolveAction(String name) {
        BusinessLogics BL = getControllerBL();
        LA<?> action;
        try {
            action = BL.findActionByCompoundName(name.replace('/', '_'));
        } catch (CompoundNameUtils.ParseException e) {
            action = null;
        }
        return action != null ? action : BL.findActionByExtId(name);
    }
    private LP<?> resolveProperty(String name) {
        BusinessLogics BL = getControllerBL();
        LP<?> property;
        try {
            property = BL.findPropertyByCompoundName(name);
        } catch (CompoundNameUtils.ParseException e) {
            property = null;
        }
        return property != null ? property : BL.findPropertyByExtId(name);
    }

    private ClientAction runControllerAction(long callbackId, LA<?> la, Object[] params, ExecutionStack stack) throws SQLException, SQLHandledException, ParseException, IOException {
        return runControllerRequest(stack, (env, context) -> {
            dropResult(la.action, env, getControllerBL().LM); // persistent (form) session may carry a stale result; harmless no-op on a fresh (navigator) session
            la.execute(env, stack, bindParams(env, la, params));
            return controllerResultAction(callbackId, context, readControllerResult(env, la.action));
        });
    }

    private ClientAction runControllerChange(long callbackId, LP<?> lp, Object[] params, Object value, ExecutionStack stack) throws SQLException, SQLHandledException, ParseException, IOException {
        return runControllerRequest(stack, (env, context) -> {
            DataObject[] keys = bindKeys(env, lp, params);
            lp.change(bindObjectValue(env, lp.property.getValueClass(ClassType.editValuePolicy), value), env, keys);
            return new ControllerResultClientAction(callbackId, null, null); // change is a pure mutation, no value -> resolve(undefined)
        });
    }

    // JSON-decoded canonical values (Number/String/Boolean/null/FileData; one per positional interface) -> typed
    // values; binding (incl. offset-ISO dates) handled inside Type.parseJSON
    private ObjectValue[] bindParams(ExecutionEnvironment env, LAP<?, ?> property, Object[] params) throws SQLException, SQLHandledException, ParseException {
        ValueClass[] classes = property.getInterfaceClasses(ClassType.parsePolicy);
        ObjectValue[] values = new ObjectValue[classes.length];
        for(int i = 0; i < classes.length; i++)
            values[i] = bindObjectValue(env, classes[i], (params != null && i < params.length) ? params[i] : null);
        return values;
    }

    private DataObject[] bindKeys(ExecutionEnvironment env, LAP<?, ?> property, Object[] params) throws SQLException, SQLHandledException, ParseException {
        DataObject[] keys = DataObject.onlyDataObjects(bindParams(env, property, params)); // change keys must be non-null object values
        if(keys == null)
            throw new RuntimeException("change requires non-null object values for keys");
        return keys;
    }

    private ObjectValue bindObjectValue(ExecutionEnvironment env, ValueClass valueClass, Object raw) throws SQLException, SQLHandledException, ParseException {
        if(valueClass == null) // unconstrained interface -- getInterfaceClasses can return null entries
            throw new RuntimeException("Unconstrained parameter type is not supported by the controller");
        // raw == null -> parseJSON returns null -> getObjectValue returns NullValue; dates handled inside Type.parseJSON
        return env.getSession().getObjectValue(valueClass, valueClass.getType().parseJSON(raw));
    }

    private ObjectValue readControllerResult(ExecutionEnvironment env, Action<?> action) throws SQLException, SQLHandledException {
        Result<SessionDataProperty> resultProp = new Result<>();
        Pair<String[], ObjectValue[]> result = readResult(action, env, getControllerBL().LM, resultProp);
        return result.second.length > 0 ? result.second[0] : null;
    }

    private ClientAction controllerResultAction(long callbackId, ConnectionContext context, ObjectValue result) throws IOException {
        if(result == null || result.getValue() == null) // no export value / null value -> JS undefined
            return new ControllerResultClientAction(callbackId, null, null);
        Object value = result.getValue();
        Type<?> type = result.getType();
        // a FILE result -> a download URL (NeedFile registers the file; the client maps the GFileType subtype to
        // getAppDownloadURL, image -> src). Structured/native data is returned via VALUE classes (JSON/JSONTEXT/XML,
        // string, number, ...) which aren't FileClass and serialize natively (e.g. GJSONType -> JSON.parse'd object).
        FormChanges.ConvertData convertData = type instanceof FileClass ? new FormChanges.NeedFile(type) : null;
        // the web reader ClientActionToGwtConverter.deserializeServerValue + convertToJSValue turn (serialized, type) into the JS value
        return new ControllerResultClientAction(callbackId, FormChanges.serializeConvertFileValue(convertData, value, context), TypeSerializer.serializeType(type));
    }

    private static String controllerMessage(Throwable t) {
        return t.getMessage() != null ? t.getMessage() : String.valueOf(t);
    }

    // symmetric readResult/dropResult (the shared base owns them; the /exec /eval HTTP path and InternalAction
    // reach them via inheritance through RemoteConnection):
    public static Pair<String[], ObjectValue[]> readResult(Action<?> action, ExecutionEnvironment env, BaseLogicsModule lm, Result<SessionDataProperty> resultProp) throws SQLException, SQLHandledException {
        return lm.getExportValueProperty().readFirstNotNull(env, resultProp, action);
    }
    // symmetric with readResult: drop the session changes of the SAME properties readResult would read (the action's
    // RETURN result props, else the "export" holder). A fresh session (navigator, /exec /eval) starts with these null;
    // a persistent session (the form controller) must drop them first or readResult returns a value left by a previous call.
    public static void dropResult(Action<?> action, ExecutionEnvironment env, BaseLogicsModule lm) throws SQLException, SQLHandledException {
        ImOrderSet<SessionDataProperty> resultProps = action.getResultProps();
        env.getSession().dropSessionChanges((!resultProps.isEmpty() ? resultProps : lm.getExportValueProperty().getProps()).getSet());
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

        @Around("execution(* lsfusion.server.base.controller.remote.RemoteRequestObject.continueServerInvocation(long, long, int, Object)) && target(object) && args(requestIndex, lastReceivedRequestIndex, continueIndex, actionResult)")
        public Object execute(ProceedingJoinPoint joinPoint, RemoteRequestObject object, long requestIndex, long lastReceivedRequestIndex, int continueIndex, final Object actionResult) throws Throwable {
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