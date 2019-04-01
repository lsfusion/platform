package lsfusion.gwt.client.controller.remote.action.form;

import java.io.Serializable;

public class ContinueInvocation extends FormRequestIndexAction<ServerResponseResult> {
    public Serializable[] actionResults;
    public int continueIndex;

    public ContinueInvocation() {
    }

    public ContinueInvocation(long requestIndex, Object[] actionResults, int continueIndex) {
        super(requestIndex);
        this.actionResults = new Serializable[actionResults.length];
        for (int i = 0; i < actionResults.length; i++) {
            this.actionResults[i] = (Serializable) actionResults[i];
        }
        this.continueIndex = continueIndex;
    }
}
