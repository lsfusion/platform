package lsfusion.gwt.client.controller.dispatch;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.URL;
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
import net.customware.gwt.dispatch.client.AbstractDispatchAsync;
import net.customware.gwt.dispatch.client.ExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchService;
import net.customware.gwt.dispatch.client.standard.StandardDispatchServiceAsync;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

import java.util.HashMap;
import java.util.Map;

public class DispatchAsyncWrapper extends AbstractDispatchAsync {
    private static boolean useGETForGwtRPC = GwtClientUtils.getPageParameter("useGETForGwtRPC") != null;

    private static StandardDispatchServiceAsync realService = null;
    
    private static Map<Action, Integer> requestTries = new HashMap<>();

    private static StandardDispatchServiceAsync getRealServiceInstance() {
        if (realService == null) {
            realService = GWT.create(StandardDispatchService.class);
            ((ServiceDefTarget) realService).setRpcRequestBuilder(new RpcRequestBuilder() {
                @Override
                protected RequestBuilder doCreate(String requestData, String serviceEntryPoint) {
                    if (useGETForGwtRPC) {
                        serviceEntryPoint += "?payload=" + URL.encodeQueryString(requestData);
                        return new RequestBuilder(RequestBuilder.GET, serviceEntryPoint);
                    } else {
                        return super.doCreate(requestData, serviceEntryPoint);
                    }
                }
            });
        }
        return realService;
    }

    public DispatchAsyncWrapper(ExceptionHandler exceptionHandler) {
        super(exceptionHandler);
    }

    public <A extends Action<R>, R extends Result> void execute(final A action, final AsyncCallback<R> callback) {
        Integer requestTry = requestTries.get(action);
        if (requestTry == null) {
            requestTry = 1;
        } else {
            requestTry++;
        }
        requestTries.put(action, requestTry);
        if (action instanceof BaseAction) {
            ((BaseAction) action).requestTry = requestTry;
        }

        final Integer finalRequestTry = requestTry;
        getRealServiceInstance().execute(action, new AsyncCallback<Result>() {
            public void onFailure(Throwable caught) {
                int maxTries = PriorityErrorHandlingCallback.getMaxTries(caught); // because we set invalidate-session to false (for some security reasons) there is no need to retry request in the case of auth problem
                boolean isAuthException = PriorityErrorHandlingCallback.isAuthException(caught);
                if (finalRequestTry <= maxTries) {
                    assert !isAuthException; // because maxTries is 0 in that case
                    if (finalRequestTry == 2) //first retry
                        GConnectionLostManager.registerFailedRmiRequest();
                    GExceptionManager.addFailedRmiRequest(caught, action);

                    Timer timer = new Timer() {  // таймер, чтобы не исчерпать слишком быстро попытки соединения с сервером
                        @Override
                        public void run() {
                            execute(action, callback);
                        }
                    };
                    timer.schedule(1000);
                } else {
                    if (maxTries > -1) // some connection problem
                        GConnectionLostManager.connectionLost(isAuthException);
                    DispatchAsyncWrapper.this.onFailure(action, caught, callback);
                }
            }

            private boolean isTooBigResult(Result result) {
                return (result instanceof ServerResponseResult && ((ServerResponseResult) result).getSize() > 20000);
            }
            public void onSuccess(Result result, boolean resultDeferred) {
                if(!resultDeferred && isTooBigResult(result)) {
                    Scheduler.get().scheduleDeferred(() -> onSuccess(result, true));
                    return;
                }
                DispatchAsyncWrapper.this.onSuccess(action, (R) result, callback);
                if(finalRequestTry > 1) //had retries
                    GConnectionLostManager.unregisterFailedRmiRequest();
                requestTries.remove(action);
                GExceptionManager.flushFailedNotFatalRequests(action);
            }

            public void onSuccess(Result result) {
                onSuccess(result, false);
            }
        });
    }
}
