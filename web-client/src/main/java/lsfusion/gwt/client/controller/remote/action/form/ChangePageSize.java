package lsfusion.gwt.client.controller.remote.action.form;

public class ChangePageSize extends FormRequestCountingAction<ServerResponseResult> {
    public int groupObjectID;
    public int pageSize;

    @SuppressWarnings("UnusedDeclaration")
    public ChangePageSize() {
    }

    public ChangePageSize(int groupObjectID, int pageSize) {
        this.groupObjectID = groupObjectID;
        this.pageSize = pageSize;
    }
}
