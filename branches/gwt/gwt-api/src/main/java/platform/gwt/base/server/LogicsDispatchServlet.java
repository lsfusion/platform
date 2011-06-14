package platform.gwt.base.server;

import net.customware.gwt.dispatch.server.DefaultActionHandlerRegistry;
import net.customware.gwt.dispatch.server.Dispatch;
import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import net.customware.gwt.dispatch.server.SimpleDispatch;
import net.customware.gwt.dispatch.server.standard.AbstractStandardDispatchServlet;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import platform.base.OSUtils;
import platform.interop.RemoteLogicsInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.rmi.RemoteException;

public abstract class LogicsDispatchServlet<T extends RemoteLogicsInterface> extends AbstractStandardDispatchServlet {
    private T logics;
    private RemoteNavigatorInterface navigator;
    private int computerId;
    protected Dispatch dispatch;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            logics = (T) GwtLogicsProvider.getLogics(getServletContext());
            computerId = logics.getComputer(OSUtils.getLocalHostName());
            //todo: make it something in the future...
            navigator = logics.createNavigator("admin", "fusion", computerId);
        } catch (RemoteException e) {
            throw new ServletException("Ошибка инициализации сервлета: ", e);
        }

        InstanceActionHandlerRegistry registry = new DefaultActionHandlerRegistry();
        addHandlers(registry);
        dispatch = new SimpleDispatch(registry);
    }

    @Override
    protected Dispatch getDispatch() {
        return dispatch;
    }

    public int getComputerId() {
        return computerId;
    }

    public T getLogics() {
        return logics;
    }

    //todo: add spring security point cut
    public synchronized RemoteNavigatorInterface getNavigator() {
        if (navigator == null) {
            try {
                Authentication auth = getAuthentication();
                String somePassword = "some";
                navigator = logics.createNavigator(auth.getName(), somePassword, getComputerId());
            } catch (RemoteException e) {
                throw new RuntimeException("Ошибка при создании навигатора: ", e);
            }
        }

        return navigator;
    }

    public HttpSession getSession() {
        return getThreadLocalRequest().getSession();
    }

    public HttpServletRequest getReqest() {
        return getThreadLocalRequest();
    }

    public HttpServletResponse getResponse() {
        return getThreadLocalResponse();
    }

    protected abstract void addHandlers(InstanceActionHandlerRegistry registry);


    protected Authentication getAuthentication() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return securityContext.getAuthentication();
    }
}
