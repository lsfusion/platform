package lsfusion.gwt.shared.form.actions.navigator;

import lsfusion.gwt.shared.result.VoidResult;

public class InterruptNavigator extends NavigatorAction<VoidResult> {
    public boolean cancelable;

    public InterruptNavigator() {
    }

    public InterruptNavigator(boolean cancelable) {
        this.cancelable = cancelable;
    }

}