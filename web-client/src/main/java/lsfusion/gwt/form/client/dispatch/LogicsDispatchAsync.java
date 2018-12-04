package lsfusion.gwt.form.client.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.form.shared.actions.logics.LogicsAction;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.shared.Result;

// part of this logics is in *RequestHandlers
public class LogicsDispatchAsync {

    private final String logicsID;
    public LogicsDispatchAsync(String logicsID) {
        this.logicsID = logicsID;
    }

    private final DispatchAsyncWrapper gwtDispatch = new DispatchAsyncWrapper(new DefaultExceptionHandler());

    public <A extends LogicsAction<R>, R extends Result> void execute(final A action, final AsyncCallback<R> callback) {
        action.logicsID = logicsID;

        gwtDispatch.execute(action, callback);
    }
}
