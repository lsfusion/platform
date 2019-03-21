package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.client.controller.remote.action.form.ChangeGroupObject;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;

import java.rmi.RemoteException;

public class ChangeGroupObjectHandler extends FormServerResponseActionHandler<ChangeGroupObject> {
    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangeGroupObjectHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangeGroupObject action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        byte[] keyValues = gwtConverter.convertOrCast(action.keyValues);

        return getServerResponseResult(form, form.remoteForm.changeGroupObject(action.requestIndex, defaultLastReceivedRequestIndex, action.groupId, keyValues));
    }
}
