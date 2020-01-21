package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ExecuteEventAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ExecuteEventActionHandler extends FormServerResponseActionHandler<ExecuteEventAction> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ExecuteEventActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final ExecuteEventAction action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                byte[] fullKey = gwtConverter.convertOrCast(action.fullKey);
                return remoteForm.executeEventAction(action.requestIndex, action.lastReceivedRequestIndex, action.propertyId, fullKey, action.actionSID);
            }
        });
    }
}
