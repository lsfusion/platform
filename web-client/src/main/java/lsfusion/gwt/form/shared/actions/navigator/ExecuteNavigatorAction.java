package lsfusion.gwt.form.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Action;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;

public class ExecuteNavigatorAction implements Action<ServerResponseResult> {
    public String actionSID;

    public ExecuteNavigatorAction() {}

    public ExecuteNavigatorAction(String actionSID) {
        this.actionSID = actionSID;
    }
}
