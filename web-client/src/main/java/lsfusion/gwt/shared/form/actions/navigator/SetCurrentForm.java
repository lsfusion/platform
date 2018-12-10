package lsfusion.gwt.shared.form.actions.navigator;

import lsfusion.gwt.shared.result.VoidResult;

public class SetCurrentForm extends NavigatorAction<VoidResult> {
    public String formID;

    public SetCurrentForm() {
    }
    public SetCurrentForm(String formID) {
        this.formID = formID;
    }
}