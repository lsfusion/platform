package lsfusion.gwt.client.navigator.controller.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.controller.dispatch.DispatchAsyncWrapper;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorAction;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.shared.Result;

public class NavigatorDispatchAsync {

    private final String sessionID;

    public NavigatorDispatchAsync(String sessionID) {
        this.sessionID = sessionID;
    }

    private final DispatchAsyncWrapper gwtDispatch = new DispatchAsyncWrapper(new DefaultExceptionHandler());

    public <A extends NavigatorAction<R>, R extends Result> void execute(final A action, final AsyncCallback<R> callback) {
        action.sessionID = sessionID;

        gwtDispatch.execute(action, callback);
    }
}
