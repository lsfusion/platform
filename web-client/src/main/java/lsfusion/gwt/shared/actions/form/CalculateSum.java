package lsfusion.gwt.shared.actions.form;

import lsfusion.gwt.shared.base.result.NumberResult;
import lsfusion.gwt.shared.form.object.GGroupObjectValue;

public class CalculateSum extends FormRequestIndexCountingAction<NumberResult> {
    public int propertyID;
    public GGroupObjectValue columnKey;

    public CalculateSum() {}

    public CalculateSum(int propertyID, GGroupObjectValue columnKey) {
        this.propertyID = propertyID;
        this.columnKey = columnKey;
    }
}
