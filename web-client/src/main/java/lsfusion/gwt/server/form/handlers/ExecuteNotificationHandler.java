package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ExecuteNotification;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ExecuteNotificationHandler extends FormServerResponseActionHandler<ExecuteNotification> {
    public ExecuteNotificationHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final ExecuteNotification action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.executeNotificationAction(action.requestIndex, action.lastReceivedRequestIndex, action.idNotification);
            }
        });
    }
}