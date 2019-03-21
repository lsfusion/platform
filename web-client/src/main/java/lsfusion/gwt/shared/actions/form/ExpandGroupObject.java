package lsfusion.gwt.shared.actions.form;

import lsfusion.gwt.shared.form.object.GGroupObjectValue;

public class ExpandGroupObject extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupObjectId;
    public GGroupObjectValue value;

    public ExpandGroupObject() {}

    public ExpandGroupObject(int groupObjectId, GGroupObjectValue value) {
        this.groupObjectId = groupObjectId;
        this.value = value;
    }

}
