package platform.gwt.form.shared.actions.form;

import platform.gwt.form.shared.view.changes.GGroupObjectValue;

public class CollapseGroupObject extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupObjectId;
    public GGroupObjectValue value;

    public CollapseGroupObject() {}

    public CollapseGroupObject(int groupObjectId, GGroupObjectValue value) {
        this.groupObjectId = groupObjectId;
        this.value = value;
    }

}
