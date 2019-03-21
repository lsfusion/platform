package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.client.controller.remote.action.navigator.GetRemoteNavigatorActionMessage;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.rmi.RemoteException;

public class GetRemoteNavigatorActionMessageHandler extends NavigatorActionHandler<GetRemoteNavigatorActionMessage, StringResult> {

    public GetRemoteNavigatorActionMessageHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    protected String getActionDetails(GetRemoteNavigatorActionMessage action) {
        return null; // too many logs
    }

    @Override
    public StringResult executeEx(GetRemoteNavigatorActionMessage action, ExecutionContext context) throws RemoteException {
        return new StringResult(getRemoteNavigator(action).getRemoteActionMessage());
    }
}
