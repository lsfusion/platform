package platform.gwt.form.shared.actions.form;

import platform.gwt.form.shared.view.changes.dto.GGroupObjectValueDTO;

import java.io.Serializable;

public class ChangeProperty extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int propertyId;
    public GGroupObjectValueDTO fullKey;
    public Serializable value;
    public Integer addedObjectId;

    public ChangeProperty() {
    }

    public ChangeProperty(int propertyId, GGroupObjectValueDTO fullKey, Serializable value, Integer addedObjectId) {
        this.propertyId = propertyId;
        this.fullKey = fullKey;
        this.value = value;
        this.addedObjectId = addedObjectId;
    }
}
