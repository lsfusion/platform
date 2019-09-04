package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.CollapseGroupObjectRecursive;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class CollapseGroupObjectRecursiveHandler extends FormServerResponseActionHandler<CollapseGroupObjectRecursive> {

    public CollapseGroupObjectRecursiveHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final CollapseGroupObjectRecursive action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.collapseGroupObjectRecursive(action.requestIndex, action.lastReceivedRequestIndex, action.groupObjectId, action.current);
            }
        });
    }
}