package lsfusion.client.navigator.controller.dispatch;

import lsfusion.client.controller.dispatch.DispatcherListener;
import lsfusion.client.controller.dispatch.SwingClientActionDispatcher;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.base.remote.PendingRemoteInterface;

import java.io.IOException;
import java.rmi.RemoteException;

public class ClientNavigatorActionDispatcher extends SwingClientActionDispatcher {
    private final ClientNavigator clientNavigator;

    public ClientNavigatorActionDispatcher(DispatcherListener dispatcherListener, ClientNavigator clientNavigator) {
        super(dispatcherListener);
        this.clientNavigator = clientNavigator;
    }

    @Override
    protected PendingRemoteInterface getRemote() {
        return clientNavigator.remoteNavigator;
    }

    @Override
    public ServerResponse continueServerInvocation(long requestIndex, int continueIndex, Object[] actionResults) throws RemoteException {
        return clientNavigator.remoteNavigator.continueNavigatorAction(actionResults);
    }

    @Override
    protected ServerResponse throwInServerInvocation(long requestIndex, int continueIndex, Throwable t) throws IOException {
        return clientNavigator.remoteNavigator.throwInNavigatorAction(t);
    }
}
