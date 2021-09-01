package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.controller.remote.action.RequestAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;

public class NavigatorRequestAction extends NavigatorAction<ServerResponseResult> implements RequestAction<ServerResponseResult> {
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
