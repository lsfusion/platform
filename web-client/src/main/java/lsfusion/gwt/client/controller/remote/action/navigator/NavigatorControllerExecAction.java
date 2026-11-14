package lsfusion.gwt.client.controller.remote.action.navigator;

import java.io.Serializable;
import java.util.ArrayList;

// navigator controller exec(action, params...): runs a system action by canonical name in the navigator's session.
public class NavigatorControllerExecAction extends NavigatorControllerRequestAction {
    public String action;
    public String options; // of the call (controller.options): a JSON object's text, null for none

    public NavigatorControllerExecAction() {
    }

    public NavigatorControllerExecAction(String action, ArrayList<Serializable> params, String options) {
        super(params);
        this.action = action;
        this.options = options;
    }
}
