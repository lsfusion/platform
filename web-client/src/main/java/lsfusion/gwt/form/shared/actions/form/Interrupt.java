package lsfusion.gwt.form.shared.actions.form;

import lsfusion.gwt.base.shared.actions.VoidResult;

public class Interrupt extends FormBoundAction<VoidResult> {
    public boolean cancelable;

    public Interrupt() {
    }

    public Interrupt(boolean cancelable) {
        this.cancelable = cancelable;
    }

}