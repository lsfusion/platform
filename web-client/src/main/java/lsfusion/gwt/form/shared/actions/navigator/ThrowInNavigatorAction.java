package lsfusion.gwt.form.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Action;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;

public class ThrowInNavigatorAction implements Action<ServerResponseResult> {
    public Throwable throwable;

    public ThrowInNavigatorAction() {}

    public ThrowInNavigatorAction(Throwable throwable) {
        this.throwable = throwable;
    }
}
