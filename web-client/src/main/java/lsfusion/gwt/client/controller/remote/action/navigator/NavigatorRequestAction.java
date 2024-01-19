package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.controller.remote.action.RequestAction;
import net.customware.gwt.dispatch.shared.Result;

public class NavigatorRequestAction<R extends Result> extends NavigatorAction<R> implements RequestAction<R> {
    public long requestIndex;
    public long lastReceivedRequestIndex;

    public NavigatorRequestAction() {
    }

    public NavigatorRequestAction(long requestIndex) {
        this.requestIndex = requestIndex;
    }

    @Override
    public String toString() {
        return super.toString() + " [request#: " + requestIndex + "]";
    }
}
