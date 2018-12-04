package lsfusion.gwt.form.client.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.form.shared.actions.logics.LogicsAction;
import lsfusion.gwt.form.shared.actions.navigator.NavigatorAction;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.shared.Result;

public class NavigatorDispatchAsync {

    private final String navigatorID;

    public NavigatorDispatchAsync(String navigatorID) {
        this.navigatorID = navigatorID;
    }

    private final DispatchAsyncWrapper gwtDispatch = new DispatchAsyncWrapper(new DefaultExceptionHandler());

    public <A extends NavigatorAction<R>, R extends Result> void execute(final A action, final AsyncCallback<R> callback) {
        action.navigatorID = navigatorID;

        gwtDispatch.execute(action, callback);
    }
}
