package lsfusion.gwt.shared.form.actions.form;


import lsfusion.gwt.shared.form.view.changes.GGroupObjectValue;

public class ChangeGroupObject extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupId;
    public GGroupObjectValue keyValues;

    public ChangeGroupObject() {
    }

    public ChangeGroupObject(int groupId, GGroupObjectValue keyValues) {
        this.groupId = groupId;
        this.keyValues = keyValues;
    }
}
