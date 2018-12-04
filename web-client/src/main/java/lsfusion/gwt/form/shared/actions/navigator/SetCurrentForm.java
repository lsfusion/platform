package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.VoidResult;

public class SetCurrentForm extends NavigatorAction<VoidResult> {
    public String formID;

    public SetCurrentForm() {
    }
    public SetCurrentForm(String formID) {
        this.formID = formID;
    }
}