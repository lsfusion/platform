package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.NavigatorAction;
import lsfusion.gwt.base.shared.actions.RequestAction;
import lsfusion.gwt.base.shared.actions.VoidResult;

public class SetCurrentForm extends RequestAction<VoidResult> implements NavigatorAction {
    public String formID;

    public SetCurrentForm() {
    }
    public SetCurrentForm(String formID) {
        this.formID = formID;
    }
}