package lsfusion.gwt.client.controller.remote.action;

import lsfusion.gwt.client.controller.remote.action.navigator.CreateNavigatorResult;
import lsfusion.gwt.client.navigator.ConnectionInfo;
import lsfusion.gwt.client.controller.remote.action.logics.LogicsAction;

public class CreateNavigatorAction extends LogicsAction<CreateNavigatorResult> {
    public ConnectionInfo connectionInfo;

    public CreateNavigatorAction() {
    }

    public CreateNavigatorAction(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }
}
