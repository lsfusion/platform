package lsfusion.gwt.client.controller.remote.action.form;

public class ExpandGroupObjectRecursive extends FormRequestCountingAction<ServerResponseResult> {
    public int groupObjectId;
    public boolean current;

    public ExpandGroupObjectRecursive() {}

    public ExpandGroupObjectRecursive(int groupObjectId, boolean current) {
        this.groupObjectId = groupObjectId;
        this.current = current;
    }
}