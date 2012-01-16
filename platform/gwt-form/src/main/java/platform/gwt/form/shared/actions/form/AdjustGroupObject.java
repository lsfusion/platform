package platform.gwt.form.shared.actions.form;

import platform.gwt.view.changes.dto.ObjectDTO;

public class AdjustGroupObject extends FormBoundAction<FormChangesResult> {
    public int groupObjectId;
    public ObjectDTO[] value;

    public AdjustGroupObject() {}

    public AdjustGroupObject(int groupObjectId, ObjectDTO[] value) {
        this.groupObjectId = groupObjectId;
        this.value = value;
    }
}
