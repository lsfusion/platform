package platform.gwt.form.shared.actions.form;

import platform.gwt.view.changes.dto.ObjectDTO;

public class ChangeGroupObject extends FormBoundAction<FormChangesResult> {
    public int groupId;
    public ObjectDTO[] keyValues;

    public ChangeGroupObject() {
    }

    public ChangeGroupObject(int groupId, ObjectDTO[] keyValues) {
        this.groupId = groupId;
        this.keyValues = keyValues;
    }
}
