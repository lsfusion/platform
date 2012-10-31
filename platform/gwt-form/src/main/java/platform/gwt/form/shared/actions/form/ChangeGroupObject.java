package platform.gwt.form.shared.actions.form;


import platform.gwt.form.shared.view.changes.dto.GGroupObjectValueDTO;

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
