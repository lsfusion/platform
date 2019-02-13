package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorServerResponseActionHandler;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.actions.navigator.ThrowInNavigatorAction;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class ThrowInNavigatorActionHandler extends NavigatorServerResponseActionHandler<ThrowInNavigatorAction> {
    public ThrowInNavigatorActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ThrowInNavigatorAction action, ExecutionContext context) throws DispatchException, IOException {
        return getServerResponseResult(action, getRemoteNavigator(action).throwInNavigatorAction(action.throwable));
    }
}
