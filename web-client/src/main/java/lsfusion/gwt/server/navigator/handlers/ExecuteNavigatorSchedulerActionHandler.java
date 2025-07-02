package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.ExecuteNavigatorSchedulerAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.navigator.NavigatorServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ExecuteNavigatorSchedulerActionHandler extends NavigatorServerResponseActionHandler<ExecuteNavigatorSchedulerAction> {
    private static final GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ExecuteNavigatorSchedulerActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ExecuteNavigatorSchedulerAction action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, getRemoteNavigator(action).executeNavigatorSchedulerAction(action.requestIndex, action.lastReceivedRequestIndex, gwtConverter.convertNavigatorScheduler(action.navigatorScheduler)));
    }
}
