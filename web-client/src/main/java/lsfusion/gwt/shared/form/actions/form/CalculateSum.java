package lsfusion.gwt.shared.form.actions.form;

import lsfusion.gwt.shared.base.actions.NumberResult;
import lsfusion.gwt.shared.form.view.changes.GGroupObjectValue;

public class CalculateSum extends FormRequestIndexCountingAction<NumberResult> {
    public int propertyID;
    public GGroupObjectValue columnKey;

    public CalculateSum() {}

    public CalculateSum(int propertyID, GGroupObjectValue columnKey) {
        this.propertyID = propertyID;
        this.columnKey = columnKey;
    }
}
