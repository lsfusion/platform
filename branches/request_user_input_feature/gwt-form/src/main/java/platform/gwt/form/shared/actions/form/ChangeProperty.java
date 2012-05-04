package platform.gwt.form.shared.actions.form;

import platform.gwt.view.changes.dto.ObjectDTO;

public class ChangeProperty extends FormBoundAction<FormChangesResult> {
    public int propertyId;
    public ObjectDTO value;

    public ChangeProperty() {
    }

    public ChangeProperty(int propertyId, Object value) {
        this.propertyId = propertyId;
        this.value = new ObjectDTO(value);
    }
}
