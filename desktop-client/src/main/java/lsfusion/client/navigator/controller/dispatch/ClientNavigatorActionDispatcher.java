package lsfusion.client.navigator.controller.dispatch;

import com.google.common.base.Throwables;
import lsfusion.client.controller.dispatch.SwingClientActionDispatcher;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.interop.action.ProcessNavigatorChangesClientAction;
import lsfusion.interop.base.remote.PendingRemoteInterface;
import lsfusion.interop.base.remote.RemoteRequestInterface;

import java.io.IOException;

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
    protected RemoteRequestInterface getRemoteRequestInterface() {
        return clientNavigator.remoteNavigator;
    }

    public void execute(ProcessNavigatorChangesClientAction action) {
        try {
            clientNavigator.applyNavigatorChanges(action.requestIndex, action.formChanges, false);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }
}
