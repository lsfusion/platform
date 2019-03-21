package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.io.Serializable;

public class ChangeProperty extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int propertyId;
    public GGroupObjectValue fullKey;
    public Serializable value;
    public Long addedObjectId;

    public ChangeProperty() {
    }

    public ChangeProperty(int propertyId, GGroupObjectValue fullKey, Serializable value, Long addedObjectId) {
        this.propertyId = propertyId;
        this.fullKey = fullKey;
        this.value = value;
        this.addedObjectId = addedObjectId;
    }
}
