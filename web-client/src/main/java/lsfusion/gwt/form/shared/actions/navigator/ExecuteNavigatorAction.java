package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.NavigatorAction;

public class ExecuteNavigatorAction extends NavigatorRequestAction implements NavigatorAction {
    public String actionSID;
    public int type;

    public ExecuteNavigatorAction() {}

    public ExecuteNavigatorAction(String tabSID, String actionSID, int type) {
        super(tabSID);
        this.actionSID = actionSID;
        this.type = type;
    }
}
