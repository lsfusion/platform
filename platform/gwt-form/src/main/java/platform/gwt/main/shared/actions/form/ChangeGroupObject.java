package platform.gwt.main.shared.actions.form;

import platform.gwt.view.changes.dto.GGroupObjectValueDTO;

public class ChangeGroupObject extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupId;
    public GGroupObjectValueDTO keyValues;

    public ChangeGroupObject() {
    }

    public ChangeGroupObject(int groupId, GGroupObjectValueDTO keyValues) {
        this.groupId = groupId;
        this.keyValues = keyValues;
    }
}
