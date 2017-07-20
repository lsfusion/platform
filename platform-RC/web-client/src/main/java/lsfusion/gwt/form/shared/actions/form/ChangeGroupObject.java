package lsfusion.gwt.form.shared.actions.form;


import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;

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
