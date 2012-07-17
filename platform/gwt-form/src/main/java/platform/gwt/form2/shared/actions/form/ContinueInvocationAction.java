package platform.gwt.form2.shared.actions.form;

import java.io.Serializable;

public class ContinueInvocationAction extends FormBoundAction<ServerResponseResult> {
    public Serializable[] actionResults;

    public ContinueInvocationAction() {
    }

    public ContinueInvocationAction(Object[] actionResults) {
        this.actionResults = new Serializable[actionResults.length];
        for (int i = 0; i < actionResults.length; i++) {
            this.actionResults[i] = (Serializable) actionResults[i];
        }
    }
}
