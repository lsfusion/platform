package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.base.result.NumberResult;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

public class CalculateSum extends FormRequestCountingAction<NumberResult> {
    public int propertyID;
    public GGroupObjectValue columnKey;

    public CalculateSum() {}

    public CalculateSum(int propertyID, GGroupObjectValue columnKey) {
        this.propertyID = propertyID;
        this.columnKey = columnKey;
    }
}