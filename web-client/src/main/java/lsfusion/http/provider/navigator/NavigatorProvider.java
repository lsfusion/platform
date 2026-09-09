package lsfusion.http.provider.navigator;

import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;

public interface NavigatorProvider {

    String createNavigator(LogicsSessionObject sessionObject, HttpServletRequest request) throws RemoteException;
    void setNavigatorPrepared(String sessionId, boolean isPrefetch); // the /main page for this navigator has been built (see MainController.processMain)
    NavigatorSessionObject getNavigatorSessionObject(String sessionID) throws SessionInvalidatedException;
    NavigatorSessionObject createOrGetNavigatorSessionObject(String sessionID, LogicsSessionObject sessionObject, HttpServletRequest request) throws RemoteException;
    void removeNavigatorSessionObject(String sessionID) throws RemoteException;

    ServerSettings getServerSettings(String sessionID) throws SessionInvalidatedException;
    
    String getSessionInfo();

    RemoteLogicsInterface getRemoteLogics();
}
