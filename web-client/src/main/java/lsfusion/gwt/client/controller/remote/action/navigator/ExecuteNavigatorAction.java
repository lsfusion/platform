package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;

public class ExecuteNavigatorAction extends NavigatorRequestCountingAction<ServerResponseResult> {
    public String actionSID;
    public int type;

    public ExecuteNavigatorAction() {}

    public ExecuteNavigatorAction(String actionSID, int type) {
        this.actionSID = actionSID;
        this.type = type;
    }
}
