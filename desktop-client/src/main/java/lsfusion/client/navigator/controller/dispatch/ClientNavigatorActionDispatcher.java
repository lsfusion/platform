package lsfusion.client.navigator.controller.dispatch;

import lsfusion.client.controller.dispatch.SwingClientActionDispatcher;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.controller.remote.RmiRequest;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.base.remote.PendingRemoteInterface;

import java.rmi.RemoteException;

public class ClientNavigatorActionDispatcher extends SwingClientActionDispatcher {
    private final ClientNavigator clientNavigator;
    RmiQueue rmiQueue;

    public ClientNavigatorActionDispatcher(RmiQueue rmiQueue, ClientNavigator clientNavigator) {
        super(rmiQueue);
        this.rmiQueue = rmiQueue;
        this.clientNavigator = clientNavigator;
    }

    @Override
    protected PendingRemoteInterface getRemote() {
        return clientNavigator.remoteNavigator;
    }

    @Override
    protected RmiQueue getRmiQueue() {
        return rmiQueue;
    }

    @Override
    protected RmiRequest<ServerResponse> getContinueServerRequest(final int continueIndex, final Object[] actionResults) {
        return new RmiRequest<ServerResponse>("continueServerInvocation") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
                return clientNavigator.remoteNavigator.continueNavigatorAction(requestIndex, lastReceivedRequestIndex, continueIndex, actionResults);
            }
        };
    }

    @Override
    protected RmiRequest<ServerResponse> getThrowInServerRequest(final int continueIndex, final Throwable clientThrowable) {
        return new RmiRequest<ServerResponse>("throwInServerInvocation") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
                return clientNavigator.remoteNavigator.throwInNavigatorAction(requestIndex, lastReceivedRequestIndex, continueIndex, clientThrowable);
            }
        };
    }
}
