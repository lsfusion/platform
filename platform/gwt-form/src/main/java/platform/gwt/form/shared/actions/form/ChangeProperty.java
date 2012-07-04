package platform.gwt.form.shared.actions.form;

import java.io.Serializable;

public class ChangeProperty extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int propertyId;
    public Serializable value;

    public ChangeProperty() {
    }

    public ChangeProperty(int propertyId, Serializable value) {
        this.propertyId = propertyId;
        this.value = value;
    }
}
