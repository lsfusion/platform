package platform.gwt.form.shared.actions.form;

import platform.gwt.view.changes.dto.GGroupObjectValueDTO;

public class CollapseGroupObject extends FormBoundAction<FormChangesResult> {
    public int groupObjectId;
    public GGroupObjectValueDTO value;

    public CollapseGroupObject() {}

    public CollapseGroupObject(int groupObjectId, GGroupObjectValueDTO value) {
        this.groupObjectId = groupObjectId;
        this.value = value;
    }

}
