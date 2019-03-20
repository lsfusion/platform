package lsfusion.http.provider.navigator;


import lsfusion.interop.logics.LogicsSessionObject;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;

public interface NavigatorProvider {

    String createNavigator(LogicsSessionObject sessionObject, HttpServletRequest request) throws RemoteException;
    NavigatorSessionObject getNavigatorSessionObject(String sessionID);
    void removeNavigatorSessionObject(String sessionID) throws RemoteException;

    String getLogicsName(String sessionID);
    
    String getSessionInfo();
}
