package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.client.controller.remote.action.form.ExecuteEditAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;

import java.rmi.RemoteException;

public class ExecuteEditActionHandler extends FormServerResponseActionHandler<ExecuteEditAction> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ExecuteEditActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ExecuteEditAction action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        byte[] fullKey = gwtConverter.convertOrCast(action.fullKey);

        return getServerResponseResult(form, form.remoteForm.executeEditAction(action.requestIndex, defaultLastReceivedRequestIndex, action.propertyId, fullKey, action.actionSID));
    }
}
