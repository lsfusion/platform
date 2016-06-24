package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.NavigatorAction;
import lsfusion.gwt.base.shared.actions.VoidResult;
import net.customware.gwt.dispatch.shared.Action;

public class SetCurrentForm implements Action<VoidResult>, NavigatorAction {
    public String formID;

    public SetCurrentForm() {
    }
    public SetCurrentForm(String formID) {
        this.formID = formID;
    }
}