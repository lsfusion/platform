package platform.gwt.form2.shared.actions.form;

import platform.gwt.form2.shared.view.changes.dto.GGroupObjectValueDTO;

public class ExecuteEditAction extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int propertyId;
    public GGroupObjectValueDTO columnKey;
    public String actionSID;

    public ExecuteEditAction() {
    }

    public ExecuteEditAction(int propertyId, GGroupObjectValueDTO columnKey, String actionSID) {
        this.propertyId = propertyId;
        this.columnKey = columnKey;
        this.actionSID = actionSID;
    }
}
