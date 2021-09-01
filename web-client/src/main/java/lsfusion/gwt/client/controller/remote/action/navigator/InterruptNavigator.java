package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.base.result.VoidResult;

public class InterruptNavigator extends NavigatorPriorityAction<VoidResult> {
    public boolean cancelable;

    public InterruptNavigator() {
    }

    public InterruptNavigator(boolean cancelable) {
        this.cancelable = cancelable;
    }

}