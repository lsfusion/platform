package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.VoidResult;

public class InterruptNavigator extends NavigatorAction<VoidResult> {
    public boolean cancelable;

    public InterruptNavigator() {
    }

    public InterruptNavigator(boolean cancelable) {
        this.cancelable = cancelable;
    }

}