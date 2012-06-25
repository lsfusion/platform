package platform.gwt.sgwtbase.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import platform.gwt.base.client.AsyncCallbackEx;

public class WrapperAsyncCallbackEx<T> extends AsyncCallbackEx<T> {
    private final AsyncCallback<T> wrapped;

    public WrapperAsyncCallbackEx(AsyncCallback<T> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void success(T result) {
        wrapped.onSuccess(result);
    }

    @Override
    public void failure(Throwable caught) {
        wrapped.onFailure(caught);
    }
}
