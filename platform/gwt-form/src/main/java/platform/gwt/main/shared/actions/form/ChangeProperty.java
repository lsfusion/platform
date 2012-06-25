package platform.gwt.main.shared.actions.form;

import platform.gwt.view.changes.dto.ObjectDTO;

public class ChangeProperty extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int propertyId;
    public ObjectDTO value;

    public ChangeProperty() {
    }

    public ChangeProperty(int propertyId, ObjectDTO value) {
        this.propertyId = propertyId;
        this.value = value;
    }
}
