package lsfusion.gwt.shared.form.actions.navigator;

import lsfusion.gwt.shared.base.actions.VoidResult;

public class InterruptNavigator extends NavigatorAction<VoidResult> {
    public boolean cancelable;

    public InterruptNavigator() {
    }

    public InterruptNavigator(boolean cancelable) {
        this.cancelable = cancelable;
    }

}