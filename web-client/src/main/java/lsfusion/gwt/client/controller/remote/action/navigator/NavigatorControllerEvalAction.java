package lsfusion.gwt.client.controller.remote.action.navigator;

import java.io.Serializable;
import java.util.ArrayList;

// navigator controller eval(script, params...): runs an lsf script in the navigator's session (always as an action,
// result via return).
public class NavigatorControllerEvalAction extends NavigatorControllerRequestAction {
    public String script;

    public NavigatorControllerEvalAction() {
    }

    public NavigatorControllerEvalAction(String script, ArrayList<Serializable> params) {
        super(params);
        this.script = script;
    }
}
