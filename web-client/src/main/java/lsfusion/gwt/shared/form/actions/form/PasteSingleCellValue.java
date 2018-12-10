package lsfusion.gwt.form.shared.actions.form;

import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;

import java.io.Serializable;

public class PasteSingleCellValue extends FormRequestIndexCountingAction<ServerResponseResult> {

    public int propertyId;
    public GGroupObjectValue fullKey;
    public Serializable value;

    @SuppressWarnings({"UnusedDeclaration"})
    public PasteSingleCellValue() {
    }

    public PasteSingleCellValue(int propertyId, GGroupObjectValue fullKey, Serializable value) {
        this.propertyId = propertyId;
        this.fullKey = fullKey;
        this.value = value;
    }
}
