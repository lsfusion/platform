package lsfusion.gwt.client.controller.remote.action.form;

import java.io.Serializable;
import java.util.ArrayList;

// form controller change(property, params..., value): changes a system property by canonical name in the
// form's session. params are the property keys; value is the canonical encoded value being written.
public class ControllerChangeAction extends ControllerRequestAction {
    public String property;
    public Serializable value;

    public ControllerChangeAction() {
    }

    public ControllerChangeAction(String property, ArrayList<Serializable> params, Serializable value) {
        super(params);
        this.property = property;
        this.value = value;
    }
}
