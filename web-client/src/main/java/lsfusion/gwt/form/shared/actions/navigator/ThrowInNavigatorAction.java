package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.NavigatorAction;

public class ThrowInNavigatorAction extends NavigatorRequestAction implements NavigatorAction {
    public Throwable throwable;

    public ThrowInNavigatorAction() {}

    public ThrowInNavigatorAction(String tabSID, Throwable throwable) {
        super(tabSID);
        this.throwable = throwable;
    }
}
