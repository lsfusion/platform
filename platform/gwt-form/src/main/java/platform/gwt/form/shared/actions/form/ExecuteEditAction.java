package platform.gwt.form.shared.actions.form;

import platform.gwt.view.changes.dto.GGroupObjectValueDTO;

public class ExecuteEditAction extends FormBoundAction<FormChangesResult> {
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
