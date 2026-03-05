package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;

import java.io.Serializable;

public class ContinueNavigatorAction extends NavigatorRequestAction<ServerResponseResult> {
    public Serializable actionResult;
    public int continueIndex;    

    public ContinueNavigatorAction() {}

    public ContinueNavigatorAction(Object actionResult, long requestIndex, int continueIndex) {
        super(requestIndex);
        this.actionResult = (Serializable) actionResult;
        this.continueIndex = continueIndex;
    }
}
