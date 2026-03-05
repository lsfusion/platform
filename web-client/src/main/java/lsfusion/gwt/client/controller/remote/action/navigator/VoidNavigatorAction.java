package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;

public class VoidNavigatorAction extends NavigatorRequestCountingAction<ServerResponseResult> {
    public long waitRequestIndex;

    public VoidNavigatorAction() {}

    public VoidNavigatorAction(long waitRequestIndex) {
        this.waitRequestIndex = waitRequestIndex;
    }
}
