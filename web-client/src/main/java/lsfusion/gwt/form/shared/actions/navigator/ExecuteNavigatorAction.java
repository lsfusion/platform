package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.NavigatorAction;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import net.customware.gwt.dispatch.shared.Action;

public class ExecuteNavigatorAction implements Action<ServerResponseResult>, NavigatorAction {
    public String actionSID;

    public ExecuteNavigatorAction() {}

    public ExecuteNavigatorAction(String actionSID) {
        this.actionSID = actionSID;
    }
}
