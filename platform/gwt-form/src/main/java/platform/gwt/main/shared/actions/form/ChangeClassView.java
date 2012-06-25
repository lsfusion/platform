package platform.gwt.main.shared.actions.form;

import platform.gwt.view.changes.dto.ObjectDTO;

public class ChangeClassView extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupObjectId;
    public ObjectDTO value;

    public ChangeClassView() {}

    public ChangeClassView(int groupObjectId, Object value) {
        this.groupObjectId = groupObjectId;
        this.value = new ObjectDTO(value);
    }
}
