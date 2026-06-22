package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ControllerChangeAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;

import java.rmi.RemoteException;

public class ControllerChangeActionHandler extends ControllerActionHandler<ControllerChangeAction> {

    public ControllerChangeActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    protected ServerResponse call(RemoteFormInterface remoteForm, ControllerChangeAction action) throws RemoteException {
        return remoteForm.change(action.requestIndex, action.lastReceivedRequestIndex, action.property, action.params.toArray(), action.value);
    }
}
