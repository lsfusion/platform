package platform.gwt.base.server;

import net.customware.gwt.dispatch.server.DefaultActionHandlerRegistry;
import net.customware.gwt.dispatch.server.Dispatch;
import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import net.customware.gwt.dispatch.server.SimpleDispatch;
import net.customware.gwt.dispatch.server.standard.AbstractStandardDispatchServlet;
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
    public T logics;
    public RemoteNavigatorInterface navigator;
    public int computerId;
    protected Dispatch dispatch;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            logics = (T) GwtLogicsProvider.getLogics(getServletContext());
            computerId = logics.getComputer(OSUtils.getLocalHostName());
            //todo: make it somethign in the future...
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
}
