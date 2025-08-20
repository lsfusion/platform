package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.form.SetPropertyActive;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class SetPropertyActiveHandler extends FormServerResponseActionHandler<SetPropertyActive> {
    public SetPropertyActiveHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final SetPropertyActive action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, remoteForm -> remoteForm.setPropertyActive(action.requestIndex, action.lastReceivedRequestIndex, action.propertyId, action.focused));
    }
}