package lsfusion.gwt.client.controller.remote.action.navigator;

public class VoidNavigatorAction extends NavigatorRequestCountingAction {
    public long waitRequestIndex;

    public VoidNavigatorAction() {}

    public VoidNavigatorAction(long waitRequestIndex) {
        this.waitRequestIndex = waitRequestIndex;
    }
}
