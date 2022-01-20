package lsfusion.gwt.client.controller.remote.action.form;

public class SetContainerCollapsed extends FormRequestCountingAction<ServerResponseResult> {
    public int containerID;
    public boolean collapsed;

    public SetContainerCollapsed() {
    }

    public SetContainerCollapsed(int containerID, boolean collapsed) {
        this.containerID = containerID;
        this.collapsed = collapsed;
    }
}
