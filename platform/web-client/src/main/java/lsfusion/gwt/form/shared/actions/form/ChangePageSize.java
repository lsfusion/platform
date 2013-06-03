package lsfusion.gwt.form.shared.actions.form;

public class ChangePageSize extends FormRequestIndexCountingAction<ServerResponseResult> {
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
