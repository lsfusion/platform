package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ControllerExecAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;

import java.rmi.RemoteException;

public class ControllerExecActionHandler extends ControllerActionHandler<ControllerExecAction> {

    public ControllerExecActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    protected ServerResponse call(RemoteFormInterface remoteForm, ControllerExecAction action) throws RemoteException {
        return remoteForm.exec(action.requestIndex, action.lastReceivedRequestIndex, action.callbackId, action.action, action.params.toArray());
    }
}
