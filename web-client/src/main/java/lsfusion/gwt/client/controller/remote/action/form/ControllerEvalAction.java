package lsfusion.gwt.client.controller.remote.action.form;

import java.io.Serializable;
import java.util.ArrayList;

// form controller eval(script, params...) / evalAction(script, params...): runs an lsf script in the form's session,
// result via return. evalAction toggles server-side parsing (true -> auto-wrap body into run(); false -> script
// defines its own run, typed params).
public class ControllerEvalAction extends ControllerRequestAction {
    public String script;
    public boolean evalAction;

    public ControllerEvalAction() {
    }

    public ControllerEvalAction(String script, boolean evalAction, ArrayList<Serializable> params) {
        super(params);
        this.script = script;
        this.evalAction = evalAction;
    }
}
