package platform.gwt.form.shared.actions.form;

import platform.gwt.form.shared.view.changes.GGroupObjectValue;

public class ExecuteEditAction extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int propertyId;
    public GGroupObjectValue columnKey;
    public String actionSID;

    public ExecuteEditAction() {
    }

    public ExecuteEditAction(int propertyId, GGroupObjectValue columnKey, String actionSID) {
        this.propertyId = propertyId;
        this.columnKey = columnKey;
        this.actionSID = actionSID;
    }
}
