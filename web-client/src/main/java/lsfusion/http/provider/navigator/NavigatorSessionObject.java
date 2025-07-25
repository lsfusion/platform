package lsfusion.http.provider.navigator;

import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.navigator.remote.ClientCallBackInterface;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;

import java.rmi.RemoteException;

public class NavigatorSessionObject {

    public final RemoteNavigatorInterface remoteNavigator;

    public final ServerSettings serverSettings; // needed for static resources (images / js / css)

    public final Boolean isOverMobile;

    public NavigatorSessionObject(RemoteNavigatorInterface remoteNavigator, ServerSettings serverSettings, Boolean isOverMobile) {
        this.remoteNavigator = remoteNavigator;
        this.serverSettings = serverSettings;
        this.isOverMobile = isOverMobile;
    }

    public ClientCallBackInterface remoteCallback; // caching
    public ClientCallBackInterface getRemoteCallback() throws RemoteException {
        if(remoteCallback == null)
            remoteCallback = remoteNavigator.getClientCallBack();
        return remoteCallback;
    }
}
