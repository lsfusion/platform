package lsfusion.gwt.client.controller.remote.action.form;

public class SetTabActive extends FormRequestCountingAction<ServerResponseResult> {
    public int tabbedPaneID;
    public int tabIndex;

    public SetTabActive() {
    }

    public SetTabActive(int tabbedPaneID, int tabIndex) {
        this.tabbedPaneID = tabbedPaneID;
        this.tabIndex = tabIndex;
    }
}
