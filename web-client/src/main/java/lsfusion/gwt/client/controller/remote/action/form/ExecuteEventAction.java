package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

public class ExecuteEventAction extends FormRequestCountingAction<ServerResponseResult> {
    public int propertyId;
    public GGroupObjectValue fullKey;
    public String actionSID;

    public ExecuteEventAction() {
    }

    public ExecuteEventAction(int propertyId, GGroupObjectValue fullKey, String actionSID) {
        this.propertyId = propertyId;
        this.fullKey = fullKey;
        this.actionSID = actionSID;
    }
}
