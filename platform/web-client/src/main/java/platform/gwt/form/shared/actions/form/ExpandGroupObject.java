package platform.gwt.form.shared.actions.form;

import platform.gwt.form.shared.view.changes.GGroupObjectValue;

public class ExpandGroupObject extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupObjectId;
    public GGroupObjectValue value;

    public ExpandGroupObject() {}

    public ExpandGroupObject(int groupObjectId, GGroupObjectValue value) {
        this.groupObjectId = groupObjectId;
        this.value = value;
    }

}
