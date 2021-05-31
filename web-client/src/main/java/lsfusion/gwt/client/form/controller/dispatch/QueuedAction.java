package lsfusion.gwt.client.form.controller.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.controller.remote.action.form.FormRequestAction;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

public class QueuedAction<R extends Result> {
    public Action<R> action;
    public AsyncCallback<R> callback;

    public boolean succeeded = false;
    public boolean finished = false;
    private R result;
    private Throwable throwable;

    public boolean flushAnyway;
    public boolean preProceeded;

    public QueuedAction(Action action, AsyncCallback callback, boolean flushAnyway) {
        this.action = action;
        this.callback = callback;
        this.flushAnyway = flushAnyway;
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

    public void proceed() {
        assert finished;

        if (succeeded) {
            callback.onSuccess(result);
        } else {
            callback.onFailure(throwable);
        }
    }

    public long getRequestIndex() {
        return action instanceof FormRequestAction ? ((FormRequestAction) action).requestIndex : -1;
    }
}
