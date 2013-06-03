package lsfusion.client.form.dispatch;

import lsfusion.client.navigator.ClientNavigator;
import lsfusion.interop.action.UpdateCurrentClassClientAction;
import lsfusion.interop.form.ServerResponse;

import java.io.IOException;
import java.rmi.RemoteException;

public class ClientNavigatorActionDispatcher extends SwingClientActionDispatcher {
    private final ClientNavigator clientNavigator;

    public ClientNavigatorActionDispatcher(ClientNavigator clientNavigator) {
        this.clientNavigator = clientNavigator;
    }

    @Override
    public ServerResponse continueServerInvocation(Object[] actionResults) throws RemoteException {
        return clientNavigator.remoteNavigator.continueNavigatorAction(actionResults);
    }

    @Override
    protected void throwInServerInvocation(Exception ex) throws IOException {
        clientNavigator.remoteNavigator.throwInNavigatorAction(ex);
    }

    @Override
    public void execute(UpdateCurrentClassClientAction action) {
        clientNavigator.relevantClassNavigator.updateCurrentClass(action.currentClassId);
    }
}
