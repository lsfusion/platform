package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.NavigatorAction;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import net.customware.gwt.dispatch.shared.Action;

import java.io.Serializable;

public class ContinueNavigatorAction implements Action<ServerResponseResult>, NavigatorAction {
    public Serializable[] actionResults;

    public ContinueNavigatorAction() {}

    public ContinueNavigatorAction(Object[] actionResults) {
        this.actionResults = new Serializable[actionResults.length];
        for (int i = 0; i < actionResults.length; i++) {
            this.actionResults[i] = (Serializable) actionResults[i];
        }
    }
}
