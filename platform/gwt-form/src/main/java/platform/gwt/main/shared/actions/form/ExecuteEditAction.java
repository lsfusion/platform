package platform.gwt.main.shared.actions.form;

import platform.gwt.view.changes.dto.GGroupObjectValueDTO;

public class ExecuteEditAction extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int propertyId;
    public GGroupObjectValueDTO key;
    public String actionSID;

    public ExecuteEditAction() {
    }

    public ExecuteEditAction(int propertyId, GGroupObjectValueDTO key, String actionSID) {
        this.propertyId = propertyId;
        this.key = key;
        this.actionSID = actionSID;
    }
}
