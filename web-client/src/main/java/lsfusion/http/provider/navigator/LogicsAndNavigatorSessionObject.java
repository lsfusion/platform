package lsfusion.http.provider.navigator;

import lsfusion.gwt.shared.GwtSharedUtils;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;

import java.rmi.RemoteException;

public class LogicsAndNavigatorSessionObject {

    public final RemoteLogicsInterface remoteLogics;
    public final RemoteNavigatorInterface remoteNavigator;
    
    public final String logicsName;

    public LogicsAndNavigatorSessionObject(RemoteLogicsInterface remoteLogics, RemoteNavigatorInterface remoteNavigator, String logicsName) {
        this.remoteLogics = remoteLogics;
        this.remoteNavigator = remoteNavigator;
        
        this.logicsName = logicsName;
    }

    public ClientCallBackInterface remoteCallback; // caching
    public ClientCallBackInterface getRemoteCallback() throws RemoteException {
        if(remoteCallback == null)
            remoteCallback = remoteNavigator.getClientCallBack();
        return remoteCallback;
    }


    public String servSID = GwtSharedUtils.randomString(25);
}
