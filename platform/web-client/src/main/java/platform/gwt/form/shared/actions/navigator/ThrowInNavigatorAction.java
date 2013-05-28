package platform.gwt.form.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.form.shared.actions.form.ServerResponseResult;

public class ThrowInNavigatorAction implements Action<ServerResponseResult> {
    public Exception exception;

    public ThrowInNavigatorAction() {}

    public ThrowInNavigatorAction(Exception exception) {
        this.exception = exception;
    }
}
