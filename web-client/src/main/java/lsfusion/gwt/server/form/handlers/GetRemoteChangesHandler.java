package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.GetRemoteChanges;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class GetRemoteChangesHandler extends FormServerResponseActionHandler<GetRemoteChanges> {
    public GetRemoteChangesHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(GetRemoteChanges action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.getRemoteChanges(action.requestIndex, defaultLastReceivedRequestIndex, action.refresh));
    }
}
