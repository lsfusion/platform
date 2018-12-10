package lsfusion.gwt.server.form.navigator.provider;

import lsfusion.interop.RemoteLogicsInterface;

import java.rmi.RemoteException;

public interface LogicsAndNavigatorProvider {

    String createNavigator(RemoteLogicsInterface remoteLogics) throws RemoteException;
    LogicsAndNavigatorSessionObject getLogicsAndNavigatorSessionObject(String sessionID);
    void removeLogicsAndNavigatorSessionObject(String sessionID) throws RemoteException;

    String getSessionInfo();
}
