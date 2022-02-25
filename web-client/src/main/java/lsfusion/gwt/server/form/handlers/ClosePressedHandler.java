package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ClosePressed;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ClosePressedHandler extends FormServerResponseActionHandler<ClosePressed> {
    public ClosePressedHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final ClosePressed action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, remoteForm -> remoteForm.closedPressed(action.requestIndex, action.lastReceivedRequestIndex, action.ok));
    }
}
