package lsfusion.http.provider.navigator;

import lsfusion.interop.navigator.remote.ClientCallBackInterface;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;

import java.rmi.RemoteException;

public class NavigatorSessionObject {

    public final RemoteNavigatorInterface remoteNavigator;

    public final String logicsName; // needed for static resources (images)
    
    public NavigatorSessionObject(RemoteNavigatorInterface remoteNavigator, String logicsName) {
        this.remoteNavigator = remoteNavigator;
        this.logicsName = logicsName;
    }

    public ClientCallBackInterface remoteCallback; // caching
    public ClientCallBackInterface getRemoteCallback() throws RemoteException {
        if(remoteCallback == null)
            remoteCallback = remoteNavigator.getClientCallBack();
        return remoteCallback;
    }
}
