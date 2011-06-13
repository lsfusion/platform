package platform.gwt.base.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import platform.base.OSUtils;
import platform.interop.RemoteLogicsInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.rmi.RemoteException;

public abstract class LogicsServiceServlet<T extends RemoteLogicsInterface>  extends RemoteServiceServlet {
    public T logics;
    public RemoteNavigatorInterface navigator;
    public int computerId;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            logics = (T) GwtLogicsProvider.getLogics(getServletContext());
            computerId = logics.getComputer(OSUtils.getLocalHostName());
            //todo: make it somethign in the future...
//            navigator = logics.createNavigator("admin", "fusion", computerId);
        } catch (RemoteException e) {
            throw new ServletException("Ошибка инициализации сервлета: ", e);
        }
    }


    protected Authentication getAuthentication() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return securityContext.getAuthentication();
    }
}
