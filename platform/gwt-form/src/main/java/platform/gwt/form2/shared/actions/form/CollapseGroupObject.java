package platform.gwt.form2.shared.actions.form;

import platform.gwt.view2.changes.dto.GGroupObjectValueDTO;

public class CollapseGroupObject extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupObjectId;
    public GGroupObjectValueDTO value;

    public CollapseGroupObject() {}

    public CollapseGroupObject(int groupObjectId, GGroupObjectValueDTO value) {
        this.groupObjectId = groupObjectId;
        this.value = value;
    }

}
