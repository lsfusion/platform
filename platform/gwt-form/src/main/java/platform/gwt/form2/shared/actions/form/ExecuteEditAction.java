package platform.gwt.form2.shared.actions.form;

import platform.gwt.view2.changes.dto.GGroupObjectValueDTO;

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
