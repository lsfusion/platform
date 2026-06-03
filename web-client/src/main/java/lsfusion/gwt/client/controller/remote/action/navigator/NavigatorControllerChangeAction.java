package lsfusion.gwt.client.controller.remote.action.navigator;

import java.io.Serializable;
import java.util.ArrayList;

// navigator controller change(property, params..., value): changes a system property by canonical name in the
// navigator's session. params are the property keys; value is the canonical encoded value being written.
public class NavigatorControllerChangeAction extends NavigatorControllerRequestAction {
    public String property;
    public Serializable value;

    public NavigatorControllerChangeAction() {
    }

    public NavigatorControllerChangeAction(String property, ArrayList<Serializable> params, Serializable value) {
        super(params);
        this.property = property;
        this.value = value;
    }
}
