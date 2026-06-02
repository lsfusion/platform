package lsfusion.gwt.client.controller.remote.action.form;

import java.io.Serializable;
import java.util.ArrayList;

// form controller eval(script, params...): runs an lsf script in the form's session (always as an action,
// result via return; server hardcodes evaluateRun(script, true)).
public class ControllerEvalAction extends ControllerRequestAction {
    public String script;

    public ControllerEvalAction() {
    }

    public ControllerEvalAction(String script, ArrayList<Serializable> params) {
        super(params);
        this.script = script;
    }
}
