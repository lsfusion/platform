package lsfusion.gwt.form.client.dispatch;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import lsfusion.gwt.base.client.GwtClientUtils;
import net.customware.gwt.dispatch.client.AbstractDispatchAsync;
import net.customware.gwt.dispatch.client.ExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchService;
import net.customware.gwt.dispatch.client.standard.StandardDispatchServiceAsync;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

public class DispatchAsyncWrapper extends AbstractDispatchAsync {
    private static boolean useGETForGwtRPC = GwtClientUtils.getPageParameter("useGETForGwtRPC") != null;

    private static StandardDispatchServiceAsync realService = null;

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
        getRealServiceInstance().execute(action, new AsyncCallback<Result>() {
            public void onFailure(Throwable caught) {
                DispatchAsyncWrapper.this.onFailure(action, caught, callback);
            }

            public void onSuccess(Result result) {
                DispatchAsyncWrapper.this.onSuccess(action, (R) result, callback);
            }
        });
    }
}
