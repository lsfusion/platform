package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.NavigatorAction;
import lsfusion.gwt.base.shared.actions.RequestAction;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;

public class ThrowInNavigatorAction extends RequestAction<ServerResponseResult> implements NavigatorAction {
    public Throwable throwable;

    public ThrowInNavigatorAction() {}

    public ThrowInNavigatorAction(Throwable throwable) {
        this.throwable = throwable;
    }
}
