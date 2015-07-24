package lsfusion.gwt.form.client.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.form.shared.actions.form.FormRequestIndexCountingAction;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

public class QueuedAction<R extends Result> {
    public Action<R> action;
    public AsyncCallback<R> callback;

    public boolean succeded = false;
    public boolean finished = false;
    private R result;
    private Throwable throwable;

    public QueuedAction(Action action, AsyncCallback callback) {
        this.action = action;
        this.callback = callback;
    }

    public void succeded(R result) {
        finished = true;
        succeded = true;
        this.result = result;
    }

    public void failed(Throwable t) {
        finished = true;
        succeded = false;
        this.throwable = t;
    }

    public Throwable procceed() {
        assert finished;

        if (succeded) {
            callback.onSuccess(result);
        } else {
            callback.onFailure(throwable);
        }
        return throwable;
    }

    public int getRequestIndex() {
        return action instanceof FormRequestIndexCountingAction ? ((FormRequestIndexCountingAction) action).requestIndex : -1;
    }
}
