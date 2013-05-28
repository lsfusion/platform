package platform.gwt.form.shared.actions.form;

import platform.gwt.base.shared.actions.NumberResult;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;

public class CalculateSum extends FormRequestIndexCountingAction<NumberResult> {
    public int propertyID;
    public GGroupObjectValue columnKey;

    public CalculateSum() {}

    public CalculateSum(int propertyID, GGroupObjectValue columnKey) {
        this.propertyID = propertyID;
        this.columnKey = columnKey;
    }
}
