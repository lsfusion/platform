package platform.gwt.form2.shared.actions.form;

public class ClearPropertyOrders extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupObjectID;

    public ClearPropertyOrders() {}

    public ClearPropertyOrders(int groupObjectID) {
        this.groupObjectID = groupObjectID;
    }
}
