package platform.gwt.form.shared.actions.form;

import platform.gwt.form.shared.view.changes.GGroupObjectValue;

import java.io.Serializable;

public class ChangeProperty extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int propertyId;
    public GGroupObjectValue fullKey;
    public Serializable value;
    public Integer addedObjectId;

    public ChangeProperty() {
    }

    public ChangeProperty(int propertyId, GGroupObjectValue fullKey, Serializable value, Integer addedObjectId) {
        this.propertyId = propertyId;
        this.fullKey = fullKey;
        this.value = value;
        this.addedObjectId = addedObjectId;
    }
}
