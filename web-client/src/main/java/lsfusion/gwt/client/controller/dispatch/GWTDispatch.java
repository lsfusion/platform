package lsfusion.gwt.client.controller.dispatch;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.exception.GExceptionManager;
import lsfusion.gwt.client.controller.remote.GConnectionLostManager;
import lsfusion.gwt.client.controller.remote.action.BaseAction;
import lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import net.customware.gwt.dispatch.client.standard.StandardDispatchService;
import net.customware.gwt.dispatch.client.standard.StandardDispatchServiceAsync;
import net.customware.gwt.dispatch.shared.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GWTDispatch {
    private static boolean useGETForGwtRPC = GwtClientUtils.getPageParameter("useGETForGwtRPC") != null;

    private static StandardDispatchServiceAsync realService = null;

//    private static ExecutingAction executingAction;
    private static Request executingRequest;

    private static StandardDispatchServiceAsync getRealServiceInstance() {
        if (realService == null) {
            realService = GWT.create(StandardDispatchService.class);
            ((ServiceDefTarget) realService).setRpcRequestBuilder(new RpcRequestBuilder() {
                @Override
                protected RequestBuilder doCreate(String requestData, String serviceEntryPoint) {
                    RequestBuilder.Method methodType = RequestBuilder.POST;
                    if (useGETForGwtRPC) {
                        serviceEntryPoint += "?payload=" + URL.encodeQueryString(requestData);
                        methodType = RequestBuilder.GET;
                    }
//                    serviceEntryPoint += "?reqid=" + executingAction.idn;
                    return new RequestBuilder(methodType, serviceEntryPoint) {
                        @Override
                        public Request send() throws RequestException {
                            Request request = super.send();
                            assert executingRequest == null;
                            executingRequest = request;
                            return request;
                        }
                    };
                }
            });
        }
        return realService;
    }

//    private int idCounter;

    private class ExecutingAction<A extends BaseAction<R>, R extends Result> {
        public final A action;
        public final Supplier<Integer> priority;
        public final Object id;
        public final AsyncCallback<R> callback;

//        private final int idn;

        public ExecutingAction(A action, Supplier<Integer> priority, Object id, AsyncCallback<R> callback) {
            this.action = action;
            this.priority = priority;
            this.id = id;
            this.callback = callback;

//            idn = idCounter++;
        }

        // execution attributes
        public Request request;
        public Timer timer;
        private boolean canceled;

        public void cancel() {
            if(request == null) { // might be timered for the retry
                timer.cancel();
            } else {
                assert timer == null;
                request.cancel();
                canceled = true;
            }
        }

        public void execute() {
            canceled = false;

//            executingAction = this;

            final Integer finalRequestTry = ++action.requestTry;
            getRealServiceInstance().execute(action, new AsyncCallback<Result>() {
                public void onFailure(Throwable caught) {
                    request = null;
                    if(canceled) { // in theory onFailure shouldn't be called if the request is canceled, but just in case
                        assert false;
                        return;
                    }

                    int maxTries = PriorityErrorHandlingCallback.getMaxTries(caught); // because we set invalidate-session to false (for some security reasons) there is no need to retry request in the case of auth problem
                    boolean isAuthException = PriorityErrorHandlingCallback.isAuthException(caught);
                    if (finalRequestTry <= maxTries) {
                        assert !isAuthException; // because maxTries is 0 in that case
                        if (finalRequestTry == 2) //first retry
                            GConnectionLostManager.registerFailedRmiRequest();
                        GExceptionManager.addFailedRmiRequest(caught, action);

                        timer = new Timer() { // timer is needed, because server can be unavailable
                            @Override
                            public void run() {
                                timer = null;
                                execute();
                            }
                        };
                        timer.schedule(1000);
                    } else {
                        GWTDispatch.this.onExecuted(ExecutingAction.this);

                        if (maxTries > -1) // some connection problem
                            GConnectionLostManager.connectionLost(isAuthException);

                        callback.onFailure(caught);

                        onExecutedAfterHandling();
                    }
                }

                private boolean isTooBigResult(Result result) {
                    return (result instanceof ServerResponseResult && ((ServerResponseResult) result).getSize() > 20000);
                }
                private void onSuccess(Result result, boolean resultDeferred) {
                    if(!resultDeferred && isTooBigResult(result)) {
                        Scheduler.get().scheduleDeferred(() -> onSuccess(result, true));
                        return;
                    }

                    callback.onSuccess((R) result);

                    onExecutedAfterHandling();
                }

                public void onSuccess(Result result) {
                    request = null;

                    onExecuted(ExecutingAction.this);

                    if(finalRequestTry > 1) //had retries
                        GConnectionLostManager.unregisterFailedRmiRequest();
                    GExceptionManager.flushFailedNotFatalRequests(action);

                    onSuccess(result, false);
                }
            });

            request = executingRequest;
            executingRequest = null;
        }
    }

    private GWTDispatch() {    }

    public static final GWTDispatch instance = new GWTDispatch();

    private final ArrayList<ExecutingAction> executingActions = new ArrayList<>();
    private final ArrayList<ExecutingAction> pendingActions = new ArrayList<>();

    private int findPendingActionById(Object id) {
        for (int i = 0, pendingActionsSize = pendingActions.size(); i < pendingActionsSize; i++) {
            ExecutingAction pendingAction = pendingActions.get(i);
            if (pendingAction.id.equals(id))
                return i;
        }
        return -1;
    }
    private int findMaxPendingAction() {
        long maxPendingPriority = Long.MIN_VALUE;
        int maxPendingIndex = 0;
        for (int i = 0, pendingActionsSize = pendingActions.size(); i < pendingActionsSize; i++) {
            ExecutingAction<?, ?> pendingAction = pendingActions.get(i);
            long executingPriority = pendingAction.priority.get();
            if (executingPriority > maxPendingPriority) {
                maxPendingPriority = executingPriority;
                maxPendingIndex = i;
            }
        }
        return maxPendingIndex;
    }
    public int findMinExecutingAction(lsfusion.gwt.client.base.Result<Long> minPriority) {
        long minExecutingPriority = Long.MAX_VALUE;
        int minExecutingIndex = 0;
        for (int i = 0, executingActionsSize = executingActions.size(); i < executingActionsSize; i++) {
            ExecutingAction<?, ?> executingAction = executingActions.get(i);
            long executingPriority = executingAction.priority.get();
            if (executingPriority < minExecutingPriority) {
                minExecutingPriority = executingPriority;
                minExecutingIndex = i;
            }
        }
        minPriority.set(minExecutingPriority);
        return minExecutingIndex;
    }

    // actually the limit is 8
    // test case (interpreter gainedFocus gives 8 pending requests)
//    showTestDockedForms() {
//        FOR iterate(i, 1, 8) DO
//          SHOW test1 DOCKED;
//    }
    // but since the limit is scoped to the server address just in case we'll have the lower threshold
    private final int maxExecutingActions = 5;

    private <A extends BaseAction<R>, R extends Result> boolean tryPushExecute(ExecutingAction<A, R> newExecutingAction) {
        lsfusion.gwt.client.base.Result<Long> minPriority = new lsfusion.gwt.client.base.Result<>();
        int minExecutingIndex = findMinExecutingAction(minPriority);

        if(minPriority.result < newExecutingAction.priority.get()) {
            ExecutingAction executingAction = executingActions.get(minExecutingIndex);
            executingAction.cancel();
            pendingActions.add(executingAction);

            executingActions.set(minExecutingIndex, newExecutingAction);
            newExecutingAction.execute();
            return true;
        }

        pendingActions.add(newExecutingAction);
        return false;
    }

    // assert that ids are ordered descending by the priority
    public void onPriorityIncreased(List<?> ids) {
        onPriorityIncreased(ids, 0);
    }
    private void onPriorityIncreased(List<?> ids, int index) {
        if(index >= ids.size())
            return;

        int pendingActionIndex = findPendingActionById(ids.get(index));
        if(pendingActionIndex >= 0) { // it's pending (not executing)
            ExecutingAction pendingAction = pendingActions.remove(pendingActionIndex);
            if(!tryPushExecute(pendingAction)) // rest has even lower priority (because ids are ordered descending by the priority)
                return;
        }

        onPriorityIncreased(ids, index + 1);
    }

    public <A extends BaseAction<R>, R extends Result> void execute(final A action, Supplier<Integer> priority, Object id, final AsyncCallback<R> callback) {
        ExecutingAction<A, R> newExecutingAction = new ExecutingAction<>(action, priority, id, callback);
        if(executingActions.size() < maxExecutingActions) {
            // it's not true since there is a gap between onExecuted and onExecutedAfterHandling
//            assert pendingActions.isEmpty();
//            executingActions.add(newExecutingAction);
//            newExecutingAction.execute();
            // so we have to add this action to pendingActions, and than flushMax
            pendingActions.add(newExecutingAction);
            flushMaxPendingAction();
        } else
            tryPushExecute(newExecutingAction);
    }

    // we want to split it since during onSuccess (onFailure) more prioritized actions can be executed, so don't want to
    private <A extends BaseAction<R>, R extends Result> void onExecuted(ExecutingAction<A, R> oldExecutingAction) {
        boolean removed = executingActions.remove(oldExecutingAction);
        assert removed;
    }
    private void onExecutedAfterHandling() {
        if(pendingActions.isEmpty())
            return;

        // we need to check since some action might be already executed
        if(executingActions.size() < maxExecutingActions)
            flushMaxPendingAction();
    }

    private void flushMaxPendingAction() {
        int maxPendingIndex = findMaxPendingAction();

        ExecutingAction newExecutingAction = pendingActions.remove(maxPendingIndex);
        executingActions.add(newExecutingAction);
        newExecutingAction.execute();
    }
}
