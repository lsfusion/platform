package lsfusion.gwt.server.navigator.provider;

import lsfusion.gwt.shared.base.GwtSharedUtils;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;

import java.rmi.RemoteException;

public class LogicsAndNavigatorSessionObject {

    public final RemoteLogicsInterface remoteLogics;
    public final RemoteNavigatorInterface remoteNavigator;

    public LogicsAndNavigatorSessionObject(RemoteLogicsInterface remoteLogics, RemoteNavigatorInterface remoteNavigator) {
        this.remoteLogics = remoteLogics;
        this.remoteNavigator = remoteNavigator;
    }

    public ClientCallBackInterface remoteCallback; // caching
    public ClientCallBackInterface getRemoteCallback() throws RemoteException {
        if(remoteCallback == null)
            remoteCallback = remoteNavigator.getClientCallBack();
        return remoteCallback;
    }


    public String servSID = GwtSharedUtils.randomString(25);
}
