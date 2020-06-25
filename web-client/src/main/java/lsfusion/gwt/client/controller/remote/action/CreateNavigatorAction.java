package lsfusion.gwt.client.controller.remote.action;

import lsfusion.gwt.client.navigator.ConnectionInfo;
import lsfusion.gwt.client.controller.remote.action.logics.LogicsAction;
import net.customware.gwt.dispatch.shared.general.StringResult;

public class CreateNavigatorAction extends LogicsAction<StringResult> {
    public ConnectionInfo connectionInfo;

    public CreateNavigatorAction() {
    }

    public CreateNavigatorAction(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }
}
