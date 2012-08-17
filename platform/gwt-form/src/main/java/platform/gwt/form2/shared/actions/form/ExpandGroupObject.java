package platform.gwt.form2.shared.actions.form;

import platform.gwt.form2.shared.view.changes.dto.GGroupObjectValueDTO;

public class ExpandGroupObject extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupObjectId;
    public GGroupObjectValueDTO value;

    public ExpandGroupObject() {}

    public ExpandGroupObject(int groupObjectId, GGroupObjectValueDTO value) {
        this.groupObjectId = groupObjectId;
        this.value = value;
    }

}
