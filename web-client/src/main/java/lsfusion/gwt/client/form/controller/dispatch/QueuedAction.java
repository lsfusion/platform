package lsfusion.gwt.client.form.controller.dispatch;

import lsfusion.gwt.client.controller.remote.action.RequestAsyncCallback;
import net.customware.gwt.dispatch.shared.Result;

public class QueuedAction<R extends Result> {
    public RequestAsyncCallback<R> callback;

    public long requestIndex;

    public boolean succeeded = false;
    public boolean finished = false;
    private R result;
    private Throwable throwable;

    public Boolean preProceeded;

    public QueuedAction(long requestIndex, RequestAsyncCallback callback, boolean preProceed) {
        this.requestIndex = requestIndex;
        this.callback = callback;
        this.preProceeded = preProceed ? false : null;
    }

    public void succeeded(R result) {
        finished = true;
        succeeded = true;
        this.result = result;
    }

    public void failed(Throwable t) {
        finished = true;
        succeeded = false;
        this.throwable = t;
    }

    public void proceed(Runnable onDispatchFinished) {
        assert finished;

        if (succeeded) {
            callback.onSuccess(result, onDispatchFinished);
        } else {
            callback.onFailure(throwable);
            if(onDispatchFinished != null)
                onDispatchFinished.run();
        }
    }
}
