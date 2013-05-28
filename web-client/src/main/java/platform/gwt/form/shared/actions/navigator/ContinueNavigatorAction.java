package platform.gwt.form.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.Serializable;

public class ContinueNavigatorAction implements Action<ServerResponseResult> {
    public Serializable[] actionResults;

    public ContinueNavigatorAction() {}

    public ContinueNavigatorAction(Object[] actionResults) {
        this.actionResults = new Serializable[actionResults.length];
        for (int i = 0; i < actionResults.length; i++) {
            this.actionResults[i] = (Serializable) actionResults[i];
        }
    }
}
