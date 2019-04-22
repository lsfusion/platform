package lsfusion.gwt.client.navigator.controller.dispatch;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.RemoteDispatchAsync;
import lsfusion.gwt.client.controller.remote.action.RequestAction;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorAction;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorRequestAction;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorRequestCountingAction;
import net.customware.gwt.dispatch.shared.Result;

public class NavigatorDispatchAsync extends RemoteDispatchAsync {

    private final String sessionID;

    public NavigatorDispatchAsync(String sessionID) {
        this.sessionID = sessionID;
    }

    public <A extends NavigatorAction<R>, R extends Result> void execute(final A action, final AsyncCallback<R> callback) {
        execute(action, callback, false);
    }

    @Override
    protected <A extends RequestAction<R>, R extends Result> void fillAction(A action) {
        ((NavigatorAction) action).sessionID = sessionID;
        if (action instanceof NavigatorRequestAction) {
            if(action instanceof NavigatorRequestCountingAction)
                ((NavigatorRequestCountingAction) action).requestIndex = nextRequestIndex++;
            ((NavigatorRequestAction) action).lastReceivedRequestIndex = lastReceivedRequestIndex;
        }
    }
}
