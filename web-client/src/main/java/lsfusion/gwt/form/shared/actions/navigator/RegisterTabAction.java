package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.NavigatorAction;
import lsfusion.gwt.base.shared.actions.RequestAction;
import lsfusion.gwt.base.shared.actions.VoidResult;

public class RegisterTabAction extends RequestAction<VoidResult> implements NavigatorAction {
    public String tabSID;
    
    public RegisterTabAction() {}
    
    public RegisterTabAction(String tabSID) {
        this.tabSID = tabSID;
    }
}
