package lsfusion.client.form.dispatch;

import lsfusion.client.form.DispatcherListener;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.interop.action.ServerResponse;

import java.io.IOException;
import java.rmi.RemoteException;

public class ClientNavigatorActionDispatcher extends SwingClientActionDispatcher {
    private final ClientNavigator clientNavigator;

    public ClientNavigatorActionDispatcher(DispatcherListener dispatcherListener, ClientNavigator clientNavigator) {
        super(dispatcherListener);
        this.clientNavigator = clientNavigator;
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
