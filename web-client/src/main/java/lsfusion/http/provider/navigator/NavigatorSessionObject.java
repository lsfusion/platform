package lsfusion.http.provider.navigator;

import lsfusion.gwt.shared.GwtSharedUtils;
import lsfusion.http.provider.logics.LogicsSessionObject;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;

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
