package platform.client.form.dispatch;

import platform.client.navigator.ClientNavigator;
import platform.interop.action.UpdateCurrentClassClientAction;
import platform.interop.form.ServerResponse;

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
    public void execute(UpdateCurrentClassClientAction action) {
        clientNavigator.relevantClassNavigator.updateCurrentClass(action.currentClassId);
    }
}
