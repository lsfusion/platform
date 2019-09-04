package lsfusion.gwt.client.controller.remote.action.form;

public class CollapseGroupObjectRecursive extends FormRequestCountingAction<ServerResponseResult> {
    public int groupObjectId;
    public boolean current;

    public CollapseGroupObjectRecursive() {}

    public CollapseGroupObjectRecursive(int groupObjectId, boolean current) {
        this.groupObjectId = groupObjectId;
        this.current = current;
    }
}