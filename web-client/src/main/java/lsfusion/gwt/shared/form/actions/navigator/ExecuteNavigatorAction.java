package lsfusion.gwt.shared.form.actions.navigator;

public class ExecuteNavigatorAction extends NavigatorRequestAction {
    public String actionSID;
    public int type;

    public ExecuteNavigatorAction() {}

    public ExecuteNavigatorAction(String actionSID, int type) {
        this.actionSID = actionSID;
        this.type = type;
    }
}
