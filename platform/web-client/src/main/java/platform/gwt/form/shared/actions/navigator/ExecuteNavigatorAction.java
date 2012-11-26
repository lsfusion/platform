package platform.gwt.form.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.form.shared.actions.form.ServerResponseResult;

public class ExecuteNavigatorAction implements Action<ServerResponseResult> {
    public String actionSID;

    public ExecuteNavigatorAction() {}

    public ExecuteNavigatorAction(String actionSID) {
        this.actionSID = actionSID;
    }
}
