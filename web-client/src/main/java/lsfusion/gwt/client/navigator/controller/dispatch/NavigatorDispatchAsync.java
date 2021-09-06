package lsfusion.gwt.client.navigator.controller.dispatch;

import lsfusion.gwt.client.RemoteDispatchAsync;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.remote.action.BaseAction;
import lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback;
import lsfusion.gwt.client.controller.remote.action.RequestAction;
import lsfusion.gwt.client.controller.remote.action.navigator.*;
import net.customware.gwt.dispatch.shared.Result;
import net.customware.gwt.dispatch.shared.general.StringResult;

public class NavigatorDispatchAsync extends RemoteDispatchAsync {

    private final String sessionID;

    public NavigatorDispatchAsync(String sessionID) {
        this.sessionID = sessionID;
    }

    @Override
    protected <A extends BaseAction<R>, R extends Result> void fillAction(A action) {
        ((NavigatorAction) action).sessionID = sessionID;
    }

    @Override
    public void getServerActionMessage(PriorityErrorHandlingCallback<StringResult> callback) {
        executePriority(new GetRemoteNavigatorActionMessage(), callback);
    }

    @Override
    public void getServerActionMessageList(PriorityErrorHandlingCallback<ListResult> callback) {
        executePriority(new GetRemoteNavigatorActionMessageList(), callback);
    }

    @Override
    public void interrupt(boolean cancelable) {
        executePriority(new InterruptNavigator(cancelable), new PriorityErrorHandlingCallback<>());
    }

    @Override
    protected void showAsync(boolean set) {
    }

    @Override
    protected <A extends RequestAction<R>, R extends Result> long fillQueuedAction(A action) {
        NavigatorRequestAction navigatorRequestAction = (NavigatorRequestAction) action;
        if(action instanceof NavigatorRequestCountingAction)
            navigatorRequestAction.requestIndex = nextRequestIndex++;
        navigatorRequestAction.lastReceivedRequestIndex = lastReceivedRequestIndex;
        return navigatorRequestAction.requestIndex;
    }
}
