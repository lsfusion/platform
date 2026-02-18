package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

public class ChangePropertyActive extends FormRequestCountingAction<ServerResponseResult> {
    public int propertyId;
    public GGroupObjectValue columnKey;
    public boolean focused;

    public int[] changeSelectionProps;
    public GGroupObjectValue[] changeSelectionColumnKeys;
    public boolean[] changeSelectionValues;

    public ChangePropertyActive() {
    }

    public ChangePropertyActive(int propertyId, GGroupObjectValue columnKey, boolean focused, int[] changeSelectionProps, GGroupObjectValue[] changeSelectionColumnKeys, boolean[] changeSelectionValues) {
        this.propertyId = propertyId;
        this.columnKey = columnKey;
        this.focused = focused;
        this.changeSelectionProps = changeSelectionProps;
        this.changeSelectionColumnKeys = changeSelectionColumnKeys;
        this.changeSelectionValues = changeSelectionValues;
    }
}
