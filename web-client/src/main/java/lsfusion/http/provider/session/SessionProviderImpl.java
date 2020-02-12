package lsfusion.http.provider.session;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.authentication.LSFAuthenticationToken;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.session.remote.RemoteSessionInterface;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// session scoped - one for one browser (! not tab)
public class SessionProviderImpl implements SessionProvider, DisposableBean {

    @Override
    public SessionSessionObject createSession(RemoteLogicsInterface remoteLogics, HttpServletRequest request, String sessionID) throws RemoteException {
        AuthenticationToken lsfToken = LSFAuthenticationToken.getAppServerToken();

        RemoteSessionInterface remoteSession = remoteLogics.createSession(lsfToken, NavigatorProviderImpl.getSessionInfo(request));

        SessionSessionObject sessionSessionObject = new SessionSessionObject(remoteSession);
        addSessionSessionObject(sessionID, sessionSessionObject);
        return sessionSessionObject;
    }

    private final Map<String, SessionSessionObject> currentSessions = new ConcurrentHashMap<>();

    private void addSessionSessionObject(String sessionID, SessionSessionObject sessionSessionObject) {
        currentSessions.put(sessionID, sessionSessionObject);
    }

    @Override
    public SessionSessionObject getSessionSessionObject(String sessionID) throws SessionInvalidatedException {
        SessionSessionObject sessionSessionObject = currentSessions.get(sessionID);
        if(sessionSessionObject == null)
            throw new SessionInvalidatedException();
        return sessionSessionObject;
    }

    @Override
    public void removeSessionSessionObject(String sessionID) throws RemoteException {
        SessionSessionObject sessionSessionObject = getSessionSessionObject(sessionID);
        currentSessions.remove(sessionID);
        sessionSessionObject.remoteSession.close();
    }

    @Override
    public void destroy() throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        MainDispatchServlet.logger.error("Destroying session for user " + (auth == null ? "UNKNOWN" : auth.getName()) + "...");
        
        for(SessionSessionObject sessionSessionObject : currentSessions.values())
            sessionSessionObject.remoteSession.close();
    }

}
