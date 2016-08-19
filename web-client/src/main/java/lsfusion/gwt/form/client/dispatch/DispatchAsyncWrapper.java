package lsfusion.gwt.form.client.dispatch;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.rpc.StatusCodeException;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.form.client.GExceptionManager;
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
    private static final Integer MAX_REQUEST_TRIES = 30;

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

        final Integer finalRequestTry = requestTry;
        getRealServiceInstance().execute(action, new AsyncCallback<Result>() {
            public void onFailure(Throwable caught) {
                if (caught instanceof StatusCodeException && finalRequestTry <= MAX_REQUEST_TRIES) { // при разрыве связи пробуем послать запрос ещё MAX_REQUEST_TRIES раз 
                    GExceptionManager.addFailedRmiRequest(caught, action);
                    
                    Timer timer = new Timer() {  // таймер, чтобы не исчерпать слишком быстро попытки соединения с сервером
                        @Override
                        public void run() {
                            execute(action, callback);
                        }
                    };
                    timer.schedule(1000);
                } else {
                    DispatchAsyncWrapper.this.onFailure(action, caught, callback);
                }
            }

            public void onSuccess(Result result) {
                DispatchAsyncWrapper.this.onSuccess(action, (R) result, callback);
                
                requestTries.remove(action);
                GExceptionManager.flushFailedNotFatalRequests(action);
            }
        });
    }
}
