package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.NavigatorAction;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import net.customware.gwt.dispatch.shared.Action;

public class ThrowInNavigatorAction implements Action<ServerResponseResult>, NavigatorAction {
    public Throwable throwable;

    public ThrowInNavigatorAction() {}

    public ThrowInNavigatorAction(Throwable throwable) {
        this.throwable = throwable;
    }
}
