package lsfusion.gwt.shared.form.actions.form;

import lsfusion.gwt.shared.actions.VoidResult;

public class Interrupt extends FormAction<VoidResult> {
    public boolean cancelable;

    public Interrupt() {
    }

    public Interrupt(boolean cancelable) {
        this.cancelable = cancelable;
    }

}