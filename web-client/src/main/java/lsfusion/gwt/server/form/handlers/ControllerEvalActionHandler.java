package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ControllerEvalAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;

import java.rmi.RemoteException;

public class ControllerEvalActionHandler extends ControllerActionHandler<ControllerEvalAction> {

    public ControllerEvalActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    protected ServerResponse call(RemoteFormInterface remoteForm, ControllerEvalAction action) throws RemoteException {
        return remoteForm.eval(action.requestIndex, action.lastReceivedRequestIndex, action.script, action.evalAction, action.params.toArray());
    }
}
