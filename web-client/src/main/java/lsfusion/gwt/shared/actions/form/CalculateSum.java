package lsfusion.gwt.shared.actions.form;

import lsfusion.gwt.shared.result.NumberResult;
import lsfusion.gwt.shared.changes.GGroupObjectValue;

public class CalculateSum extends FormRequestIndexCountingAction<NumberResult> {
    public int propertyID;
    public GGroupObjectValue columnKey;

    public CalculateSum() {}

    public CalculateSum(int propertyID, GGroupObjectValue columnKey) {
        this.propertyID = propertyID;
        this.columnKey = columnKey;
    }
}
