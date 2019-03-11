package lsfusion.http.provider.session;

import lsfusion.interop.RemoteLogicsInterface;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;

public interface SessionProvider {

    SessionSessionObject createSession(RemoteLogicsInterface remoteLogics, HttpServletRequest request, String sessionID) throws RemoteException;
    SessionSessionObject getSessionSessionObject(String sessionID);
    void removeSessionSessionObject(String sessionID) throws RemoteException;

}
