package lsfusion.gwt.client.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.shared.actions.navigator.LogicsAndNavigatorAction;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.shared.Result;

public class LogicsAndNavigatorDispatchAsync {

    private final String sessionID;

    public LogicsAndNavigatorDispatchAsync(String sessionID) {
        this.sessionID = sessionID;
    }

    private final DispatchAsyncWrapper gwtDispatch = new DispatchAsyncWrapper(new DefaultExceptionHandler());

    public <A extends LogicsAndNavigatorAction<R>, R extends Result> void execute(final A action, final AsyncCallback<R> callback) {
        action.sessionID = sessionID;

        gwtDispatch.execute(action, callback);
    }
}
