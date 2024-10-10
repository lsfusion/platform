package lsfusion.http.provider.session;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.interop.session.ExternalUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// session scoped - one for one browser (! not tab)
public class SessionProviderImpl implements SessionProvider, DisposableBean {
    public SessionProviderImpl() {}

    private final ExternalUtils.SessionContainer sessionContainer = new ExternalUtils.SessionContainer();

    @Override
    public ExternalUtils.SessionContainer getContainer() {
        return sessionContainer;
    }

    @Override
    public void destroy() throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        MainDispatchServlet.logger.error("Destroying session for user " + (auth == null ? "UNKNOWN" : auth.getName()) + "...");

        sessionContainer.destroy();
    }
}
