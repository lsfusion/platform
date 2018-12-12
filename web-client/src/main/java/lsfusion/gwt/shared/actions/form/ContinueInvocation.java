package lsfusion.gwt.shared.actions.form;

import java.io.Serializable;

public class ContinueInvocation extends FormAction<ServerResponseResult> {
    public Serializable[] actionResults;

    public ContinueInvocation() {
    }

    public ContinueInvocation(Object[] actionResults) {
        this.actionResults = new Serializable[actionResults.length];
        for (int i = 0; i < actionResults.length; i++) {
            this.actionResults[i] = (Serializable) actionResults[i];
        }
    }
}
