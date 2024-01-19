package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;

import java.io.Serializable;

public class ContinueNavigatorAction extends NavigatorRequestAction<ServerResponseResult> {
    public Serializable[] actionResults;
    public int continueIndex;    

    public ContinueNavigatorAction() {}

    public ContinueNavigatorAction(Object[] actionResults, long requestIndex, int continueIndex) {
        super(requestIndex);
        this.actionResults = new Serializable[actionResults.length];
        for (int i = 0; i < actionResults.length; i++) {
            this.actionResults[i] = (Serializable) actionResults[i];
        }
        this.continueIndex = continueIndex;
    }
}
