package lsfusion.gwt.client.controller.remote.action.form;

import java.io.Serializable;
import java.util.ArrayList;

// form controller exec(action, params...): runs a system action by canonical name in the form's session.
public class ControllerExecAction extends ControllerRequestAction {
    public String action;

    public ControllerExecAction() {
    }

    public ControllerExecAction(String action, ArrayList<Serializable> params) {
        super(params);
        this.action = action;
    }
}
