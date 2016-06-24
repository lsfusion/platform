package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.form.handlers.ServerResponseActionHandler;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.actions.navigator.ExecuteNavigatorAction;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class ExecuteNavigatorActionHandler extends ServerResponseActionHandler<ExecuteNavigatorAction> implements NavigatorActionHandler {
    public ExecuteNavigatorActionHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ExecuteNavigatorAction action, ExecutionContext context) throws DispatchException, IOException {
        return getServerResponseResult(servlet.getNavigator().executeNavigatorAction(action.actionSID, action.type));
    }
}
