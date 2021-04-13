package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.GetRemoteChanges;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class GetRemoteChangesHandler extends FormServerResponseActionHandler<GetRemoteChanges> {
    public GetRemoteChangesHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final GetRemoteChanges action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.getRemoteChanges(action.requestIndex, action.lastReceivedRequestIndex, action.refresh, action.forceLocalEvents);
            }
        });
    }
}
