package platform.gwt.form.shared.actions.form;

import platform.gwt.view.changes.dto.GGroupObjectValueDTO;

public class ChangeGroupObject extends FormBoundAction<FormChangesResult> {
    public int groupId;
    public GGroupObjectValueDTO keyValues;

    public ChangeGroupObject() {
    }

    public ChangeGroupObject(int groupId, GGroupObjectValueDTO keyValues) {
        this.groupId = groupId;
        this.keyValues = keyValues;
    }
}
