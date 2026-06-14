package lsfusion.gwt.client.controller.remote.action.form;

import java.io.Serializable;
import java.util.ArrayList;

// base for the form controller's exec/eval/change requests — each runs in the form's session and delivers its
// outcome to the JS callback registered under requestIndex (see GFORM-CONTROLLER-EXEC-EVAL-PLAN). params are the
// positional canonical encoded values (Double / Boolean / String / ISO String) — action params for exec/eval,
// property keys for change.
public abstract class ControllerRequestAction extends FormRequestCountingAction<ServerResponseResult> {
    public ArrayList<Serializable> params;

    protected ControllerRequestAction() {
    }

    protected ControllerRequestAction(ArrayList<Serializable> params) {
        this.params = params;
    }
}
