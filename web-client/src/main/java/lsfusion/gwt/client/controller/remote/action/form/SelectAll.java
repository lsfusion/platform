package lsfusion.gwt.client.controller.remote.action.form;
import lsfusion.gwt.client.form.object.GGroupObjectValue;

public class SelectAll extends FormRequestCountingAction<ServerResponseResult> {
    public int groupId;
    public int[] changeSelectionProps;
    public GGroupObjectValue[] changeSelectionColumnKeys;
    public boolean[] changeSelectionValues;

    public SelectAll() {
    }

    public SelectAll(int groupId, int[] changeSelectionProps, GGroupObjectValue[] changeSelectionColumnKeys, boolean[] changeSelectionValues) {
        this.groupId = groupId;
        this.changeSelectionProps = changeSelectionProps;
        this.changeSelectionColumnKeys = changeSelectionColumnKeys;
        this.changeSelectionValues = changeSelectionValues;
    }
}
