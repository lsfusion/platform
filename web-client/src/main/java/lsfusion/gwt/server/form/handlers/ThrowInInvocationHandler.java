package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.form.ThrowInInvocation;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
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
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.throwInServerInvocation(action.requestIndex, action.lastReceivedRequestIndex, action.continueIndex, action.throwable);
            }
        });
    }
}
