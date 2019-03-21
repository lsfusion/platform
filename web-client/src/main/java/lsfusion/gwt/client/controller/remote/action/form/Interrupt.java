package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.base.result.VoidResult;

public class Interrupt extends FormAction<VoidResult> {
    public boolean cancelable;

    public Interrupt() {
    }

    public Interrupt(boolean cancelable) {
        this.cancelable = cancelable;
    }

}