package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.base.result.VoidResult;

public class VoidNavigatorAction extends NavigatorRequestCountingAction<VoidResult> {
    public long waitRequestIndex;

    public VoidNavigatorAction() {}

    public VoidNavigatorAction(long waitRequestIndex) {
        this.waitRequestIndex = waitRequestIndex;
    }
}
