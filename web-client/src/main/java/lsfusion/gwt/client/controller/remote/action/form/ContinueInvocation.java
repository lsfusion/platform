package lsfusion.gwt.client.controller.remote.action.form;

import java.io.Serializable;

public class ContinueInvocation extends FormRequestAction<ServerResponseResult> {
    public Serializable actionResult;
    public int continueIndex;

    public ContinueInvocation() {
    }

    public ContinueInvocation(long requestIndex, Object actionResult, int continueIndex) {
        super(requestIndex);
        this.actionResult = (Serializable) actionResult;
        this.continueIndex = continueIndex;
    }
}
