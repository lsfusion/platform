package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

public class ExecuteEditAction extends FormRequestCountingAction<ServerResponseResult> {
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
