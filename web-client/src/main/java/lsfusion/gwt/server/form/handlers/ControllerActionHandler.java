package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ControllerRequestAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

// base for the form controller exec/eval/change handlers: shared executeEx wrapper, subclasses only pick the
// RemoteForm method.
public abstract class ControllerActionHandler<A extends ControllerRequestAction> extends FormServerResponseActionHandler<A> {

    public ControllerActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(A action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, remoteForm -> call(remoteForm, action));
    }

    protected abstract ServerResponse call(RemoteFormInterface remoteForm, A action) throws RemoteException;
}
