package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.NavigatorAction;

import java.io.Serializable;

public class ContinueNavigatorAction extends NavigatorRequestAction implements NavigatorAction {
    public Serializable[] actionResults;

    public ContinueNavigatorAction() {}

    public ContinueNavigatorAction(String tabSID, Object[] actionResults) {
        super(tabSID);
        this.actionResults = new Serializable[actionResults.length];
        for (int i = 0; i < actionResults.length; i++) {
            this.actionResults[i] = (Serializable) actionResults[i];
        }
    }
}
