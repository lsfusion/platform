package lsfusion.gwt.client.controller.remote.action.navigator;

import java.io.Serializable;

public class ContinueNavigatorAction extends NavigatorRequestAction {
    public Serializable[] actionResults;
    public int continueIndex;    

    public ContinueNavigatorAction() {}

    public ContinueNavigatorAction(Object[] actionResults, int continueIndex) {
        this.actionResults = new Serializable[actionResults.length];
        for (int i = 0; i < actionResults.length; i++) {
            this.actionResults[i] = (Serializable) actionResults[i];
        }
        this.continueIndex = continueIndex;
    }
}
