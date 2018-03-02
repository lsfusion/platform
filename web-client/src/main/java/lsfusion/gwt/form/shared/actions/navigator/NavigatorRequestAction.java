package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.RequestAction;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;

public class NavigatorRequestAction extends RequestAction<ServerResponseResult> {
    public String tabSID;

    public NavigatorRequestAction() {
    }

    public NavigatorRequestAction(String tabSID) {
        this.tabSID = tabSID;
    }
}
