package platform.gwt.form.shared.actions.form;

public class ClearPropertyOrders extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupObjectID;

    public ClearPropertyOrders() {}

    public ClearPropertyOrders(int groupObjectID) {
        this.groupObjectID = groupObjectID;
    }
}
