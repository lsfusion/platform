package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.io.Serializable;

public class PasteSingleCellValue extends FormRequestCountingAction<ServerResponseResult> {

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
