package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ExecuteFormEventAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ExecuteFormEventActionHandler extends FormServerResponseActionHandler<ExecuteFormEventAction> {

    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ExecuteFormEventActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final ExecuteFormEventAction action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, remoteForm -> remoteForm.executeEventAction(action.requestIndex, action.lastReceivedRequestIndex, gwtConverter.convertOrCast(action.formEvent), gwtConverter.convertOrCast(action.pushAsyncResult)));
    }
}