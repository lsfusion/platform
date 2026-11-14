package lsfusion.gwt.client.controller.remote.action.form;

import java.io.Serializable;
import java.util.ArrayList;

// form controller exec(action, params...): runs a system action by canonical name in the form's session.
public class ControllerExecAction extends ControllerRequestAction {
    public String action;
    public String options; // of the call (controller.options): a JSON object's text, null for none

    public ControllerExecAction() {
    }

    public ControllerExecAction(String action, ArrayList<Serializable> params, String options) {
        super(params);
        this.action = action;
        this.options = options;
    }
}
