package lsfusion.gwt.shared.actions.navigator;

import lsfusion.gwt.shared.base.result.VoidResult;

public class SetCurrentForm extends NavigatorAction<VoidResult> {
    public String formID;

    public SetCurrentForm() {
    }
    public SetCurrentForm(String formID) {
        this.formID = formID;
    }
}