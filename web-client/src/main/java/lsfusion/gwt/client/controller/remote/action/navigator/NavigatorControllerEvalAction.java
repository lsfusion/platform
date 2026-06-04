package lsfusion.gwt.client.controller.remote.action.navigator;

import java.io.Serializable;
import java.util.ArrayList;

// navigator controller eval(script, params...) / evalAction(script, params...): runs an lsf script in the navigator's
// session, result via return. evalAction toggles server-side parsing (true -> auto-wrap body into run(); false ->
// script defines its own run, typed params).
public class NavigatorControllerEvalAction extends NavigatorControllerRequestAction {
    public String script;
    public boolean evalAction;

    public NavigatorControllerEvalAction() {
    }

    public NavigatorControllerEvalAction(String script, boolean evalAction, ArrayList<Serializable> params) {
        super(params);
        this.script = script;
        this.evalAction = evalAction;
    }
}
