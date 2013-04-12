package platform.gwt.form.server.navigator.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.server.form.handlers.ServerResponseActionHandler;
import platform.gwt.form.shared.actions.form.ServerResponseResult;
import platform.gwt.form.shared.actions.navigator.ThrowInNavigatorAction;

import java.io.IOException;

public class ThrowInNavigatorActionHandler extends ServerResponseActionHandler<ThrowInNavigatorAction> {
    public ThrowInNavigatorActionHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ThrowInNavigatorAction action, ExecutionContext context) throws DispatchException, IOException {
        return getServerResponseResult(servlet.getNavigator().throwInNavigatorAction(action.exception));
    }
}
