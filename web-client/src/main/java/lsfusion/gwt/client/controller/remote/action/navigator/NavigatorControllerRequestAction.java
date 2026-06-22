package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;

import java.io.Serializable;
import java.util.ArrayList;

// navigator-context analog of the form ControllerRequestAction: exec/eval/change run in the navigator's session
// (a fresh DataSession per call) and deliver the outcome to the JS callback registered under requestIndex. params
// are the positional canonical encoded values (action params for exec/eval, property keys for change).
public abstract class NavigatorControllerRequestAction extends NavigatorRequestCountingAction<ServerResponseResult> {
    public ArrayList<Serializable> params;

    protected NavigatorControllerRequestAction() {
    }

    protected NavigatorControllerRequestAction(ArrayList<Serializable> params) {
        this.params = params;
    }
}
