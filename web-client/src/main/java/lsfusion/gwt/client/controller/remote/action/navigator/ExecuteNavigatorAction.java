package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.form.property.async.GPushAsyncResult;

public class ExecuteNavigatorAction extends NavigatorRequestCountingAction<ServerResponseResult> {
    public String actionSID;
    public int type;
    public GPushAsyncResult pushAsyncResult;

    public ExecuteNavigatorAction() {}

    public ExecuteNavigatorAction(String actionSID, int type, GPushAsyncResult pushAsyncResult) {
        this.actionSID = actionSID;
        this.type = type;
        this.pushAsyncResult = pushAsyncResult;
    }
}
