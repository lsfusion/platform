package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.RequestAction;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.shared.actions.form.FormBoundAction;

public class InterruptNavigator extends RequestAction<VoidResult> {
    public boolean cancelable;

    public InterruptNavigator() {
    }

    public InterruptNavigator(boolean cancelable) {
        this.cancelable = cancelable;
    }

}