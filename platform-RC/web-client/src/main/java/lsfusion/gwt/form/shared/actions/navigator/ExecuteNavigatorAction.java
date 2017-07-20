package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.NavigatorAction;
import lsfusion.gwt.base.shared.actions.RequestAction;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;

public class ExecuteNavigatorAction extends RequestAction<ServerResponseResult> implements NavigatorAction {
    public String actionSID;
    public int type;

    public ExecuteNavigatorAction() {}

    public ExecuteNavigatorAction(String actionSID, int type) {
        this.actionSID = actionSID;
        this.type = type;
    }
}
