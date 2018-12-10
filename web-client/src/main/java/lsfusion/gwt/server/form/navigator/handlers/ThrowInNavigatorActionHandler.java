package lsfusion.gwt.server.form.navigator.handlers;

import lsfusion.gwt.server.form.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.navigator.NavigatorServerResponseActionHandler;
import lsfusion.gwt.shared.form.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.form.actions.navigator.ThrowInNavigatorAction;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class ThrowInNavigatorActionHandler extends NavigatorServerResponseActionHandler<ThrowInNavigatorAction> {
    public ThrowInNavigatorActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ThrowInNavigatorAction action, ExecutionContext context) throws DispatchException, IOException {
        return getServerResponseResult(action, getRemoteNavigator(action).throwInNavigatorAction(action.throwable));
    }
}
