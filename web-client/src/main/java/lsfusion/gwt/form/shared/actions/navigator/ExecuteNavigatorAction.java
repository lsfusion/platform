package lsfusion.gwt.form.shared.actions.navigator;

public class ExecuteNavigatorAction extends NavigatorRequestAction {
    public String actionSID;
    public int type;

    public ExecuteNavigatorAction() {}

    public ExecuteNavigatorAction(String tabSID, String actionSID, int type) {
        super(tabSID);
        this.actionSID = actionSID;
        this.type = type;
    }
}
