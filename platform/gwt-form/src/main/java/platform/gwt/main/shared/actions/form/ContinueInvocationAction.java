package platform.gwt.main.shared.actions.form;

import platform.gwt.view.changes.dto.ObjectDTO;

public class ContinueInvocationAction extends FormBoundAction<ServerResponseResult> {
    public ObjectDTO[] actionResults;

    public ContinueInvocationAction() {
    }

    public ContinueInvocationAction(Object[] actionResults) {
        this.actionResults = new ObjectDTO[actionResults.length];
        for (int i = 0; i < actionResults.length; i++) {
            this.actionResults[i] = new ObjectDTO(actionResults[i]);
        }
    }
}
