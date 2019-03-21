package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

public class CollapseGroupObject extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupObjectId;
    public GGroupObjectValue value;

    public CollapseGroupObject() {}

    public CollapseGroupObject(int groupObjectId, GGroupObjectValue value) {
        this.groupObjectId = groupObjectId;
        this.value = value;
    }

}
