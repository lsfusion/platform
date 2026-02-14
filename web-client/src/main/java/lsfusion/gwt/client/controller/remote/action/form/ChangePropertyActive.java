package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.event.GChangeSelection;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.util.ArrayList;

public class ChangePropertyActive extends FormRequestCountingAction<ServerResponseResult> {
    public int propertyId;
    public GGroupObjectValue columnKey;
    public boolean focused;

    public GChangeSelection changeSelection;
    public int[] changeSelectionProps;
    public GGroupObjectValue[] changeSelectionColumnKeys;
    public boolean[] changeSelectionValues;

    public ChangePropertyActive() {
    }

    public ChangePropertyActive(int propertyId, GGroupObjectValue columnKey, boolean focused, GChangeSelection changeSelection, int[] changeSelectionProps, GGroupObjectValue[] changeSelectionColumnKeys, boolean[] changeSelectionValues) {
        this.propertyId = propertyId;
        this.columnKey = columnKey;
        this.focused = focused;
        this.changeSelection = changeSelection;
        this.changeSelectionProps = changeSelectionProps;
        this.changeSelectionColumnKeys = changeSelectionColumnKeys;
        this.changeSelectionValues = changeSelectionValues;
    }
}
