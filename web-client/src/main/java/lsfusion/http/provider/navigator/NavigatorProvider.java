package lsfusion.http.provider.navigator;

import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;

public interface NavigatorProvider {

    String createNavigator(LogicsSessionObject sessionObject, HttpServletRequest request) throws RemoteException;
    void updateNavigatorClientSettings(String screenSize, boolean mobile) throws RemoteException;
    NavigatorSessionObject getNavigatorSessionObject(String sessionID) throws SessionInvalidatedException;
    NavigatorSessionObject createOrGetNavigatorSessionObject(String sessionID, LogicsSessionObject sessionObject, HttpServletRequest request) throws RemoteException;
    void removeNavigatorSessionObject(String sessionID) throws RemoteException;

    ServerSettings getServerSettings(String sessionID) throws SessionInvalidatedException;
    
    String getSessionInfo();

    RemoteLogicsInterface getRemoteLogics();
}
