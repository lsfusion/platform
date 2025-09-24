package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ContinueInvocation;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ContinueInvocationHandler extends FormServerResponseActionHandler<ContinueInvocation> {
    private final GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ContinueInvocationHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final ContinueInvocation action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, remoteForm -> remoteForm.continueServerInvocation(action.requestIndex, action.lastReceivedRequestIndex, action.continueIndex, gwtConverter.convertOrCast(action.actionResult)));
    }
}
