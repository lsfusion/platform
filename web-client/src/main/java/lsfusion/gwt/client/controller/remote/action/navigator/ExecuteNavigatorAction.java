package lsfusion.gwt.client.controller.remote.action.navigator;

public class ExecuteNavigatorAction extends NavigatorRequestAction {
    public String actionSID;
    public int type;

    public ExecuteNavigatorAction() {}

    public ExecuteNavigatorAction(String actionSID, int type) {
        this.actionSID = actionSID;
        this.type = type;
    }
}
