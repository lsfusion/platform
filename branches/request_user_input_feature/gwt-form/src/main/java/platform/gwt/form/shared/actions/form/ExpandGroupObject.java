package platform.gwt.form.shared.actions.form;

import platform.gwt.view.changes.dto.GGroupObjectValueDTO;

public class ExpandGroupObject extends FormBoundAction<FormChangesResult> {
    public int groupObjectId;
    public GGroupObjectValueDTO value;

    public ExpandGroupObject() {}

    public ExpandGroupObject(int groupObjectId, GGroupObjectValueDTO value) {
        this.groupObjectId = groupObjectId;
        this.value = value;
    }

}
