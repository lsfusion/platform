package lsfusion.gwt.form.server.navigator.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.form.handlers.ServerResponseActionHandler;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.actions.navigator.ThrowInNavigatorAction;

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
