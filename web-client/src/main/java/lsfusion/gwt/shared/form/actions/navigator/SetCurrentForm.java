package lsfusion.gwt.shared.form.actions.navigator;

import lsfusion.gwt.shared.base.actions.VoidResult;

public class SetCurrentForm extends NavigatorAction<VoidResult> {
    public String formID;

    public SetCurrentForm() {
    }
    public SetCurrentForm(String formID) {
        this.formID = formID;
    }
}