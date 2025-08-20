package lsfusion.http.provider.navigator;

import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.navigator.remote.ClientCallBackInterface;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;

import java.rmi.RemoteException;

public class NavigatorSessionObject {

    public final RemoteNavigatorInterface remoteNavigator;

    public final ServerSettings serverSettings; // needed for static resources (images / js / css)

    public int initialized; // 0 - not yet, 1 - prefetched, 2 - initialized

    public NavigatorSessionObject(RemoteNavigatorInterface remoteNavigator, ServerSettings serverSettings) {
        this.remoteNavigator = remoteNavigator;
        this.serverSettings = serverSettings;
    }

    public ClientCallBackInterface remoteCallback; // caching
    public ClientCallBackInterface getRemoteCallback() throws RemoteException {
        if(remoteCallback == null)
            remoteCallback = remoteNavigator.getClientCallBack();
        return remoteCallback;
    }
}
