package lsfusion.gwt.shared.actions.form;

import lsfusion.gwt.shared.changes.GGroupObjectValue;

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
