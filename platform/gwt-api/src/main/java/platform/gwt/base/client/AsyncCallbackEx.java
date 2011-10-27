package platform.gwt.base.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class AsyncCallbackEx<T> implements AsyncCallback<T> {
    @Override
    public final void onSuccess(T result) {
        preProcess();
        success(result);
        postProcess();
    }

    @Override
    public final void onFailure(Throwable caught) {
        preProcess();
        failure(caught);
        postProcess();
    }

    public void preProcess() {
        //so nothing
    }

    public void success(T result) {
        //so nothing
    }

    public void failure(Throwable caught) {
        //so nothing
    }

    public void postProcess() {
        //so nothing
    }
}
