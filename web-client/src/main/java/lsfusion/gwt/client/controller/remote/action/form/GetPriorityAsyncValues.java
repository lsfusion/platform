package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

public class GetPriorityAsyncValues extends FormPriorityAction<ListResult> {
    public int propertyID;
    public GGroupObjectValue columnKey;
    public String actionSID; // extended SID ServerResponse.events + FILTER + SYNCREQUEST
    public String value;
    public int index;
    public int increaseValuesNeededCount;

    public GetPriorityAsyncValues() {
    }

    public GetPriorityAsyncValues(int propertyID, GGroupObjectValue columnKey, String actionSID, String value, int index, int increaseValuesNeededCount) {
        this.propertyID = propertyID;
        this.columnKey = columnKey;
        this.actionSID = actionSID;
        this.value = value;
        this.index = index;
        this.increaseValuesNeededCount = increaseValuesNeededCount;
    }
}
