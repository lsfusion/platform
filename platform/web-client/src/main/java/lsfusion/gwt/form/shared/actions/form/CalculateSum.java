package lsfusion.gwt.form.shared.actions.form;

import lsfusion.gwt.base.shared.actions.NumberResult;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;

public class CalculateSum extends FormRequestIndexCountingAction<NumberResult> {
    public int propertyID;
    public GGroupObjectValue columnKey;

    public CalculateSum() {}

    public CalculateSum(int propertyID, GGroupObjectValue columnKey) {
        this.propertyID = propertyID;
        this.columnKey = columnKey;
    }
}
