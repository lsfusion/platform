package lsfusion.gwt.client.controller.remote.action.form;

public class SetTabVisible extends FormRequestCountingAction<ServerResponseResult> {
    public int tabbedPaneID;
    public int tabIndex;

    public SetTabVisible() {
    }

    public SetTabVisible(int tabbedPaneID, int tabIndex) {
        this.tabbedPaneID = tabbedPaneID;
        this.tabIndex = tabIndex;
    }
}
