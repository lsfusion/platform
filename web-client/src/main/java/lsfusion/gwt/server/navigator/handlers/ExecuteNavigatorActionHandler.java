package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorServerResponseActionHandler;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.actions.navigator.ExecuteNavigatorAction;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class ExecuteNavigatorActionHandler extends NavigatorServerResponseActionHandler<ExecuteNavigatorAction> {
    public ExecuteNavigatorActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ExecuteNavigatorAction action, ExecutionContext context) throws DispatchException, IOException {
        return getServerResponseResult(action, getRemoteNavigator(action).executeNavigatorAction(action.actionSID, action.type));
    }
}
