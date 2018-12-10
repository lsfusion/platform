package lsfusion.gwt.shared.form.actions.form;

import lsfusion.gwt.shared.form.view.changes.GGroupObjectValue;

public class ExecuteEditAction extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int propertyId;
    public GGroupObjectValue fullKey;
    public String actionSID;

    public ExecuteEditAction() {
    }

    public ExecuteEditAction(int propertyId, GGroupObjectValue fullKey, String actionSID) {
        this.propertyId = propertyId;
        this.fullKey = fullKey;
        this.actionSID = actionSID;
    }
}
