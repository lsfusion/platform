package platform.gwt.form2.shared.actions.form;


import platform.gwt.view2.changes.dto.GGroupObjectValueDTO;

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
