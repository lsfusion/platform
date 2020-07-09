package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.form.ThrowInInvocation;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.gwt.server.navigator.handlers.LogClientExceptionActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ThrowInInvocationHandler extends FormServerResponseActionHandler<ThrowInInvocation> {
    public ThrowInInvocationHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final ThrowInInvocation action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, remoteForm -> remoteForm.throwInServerInvocation(action.requestIndex, action.lastReceivedRequestIndex, action.continueIndex, LogClientExceptionActionHandler.fromWebServerToAppServer(action.throwable)));
    }
}
