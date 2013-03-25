package platform.gwt.base.server;

import net.customware.gwt.dispatch.server.DefaultActionHandlerRegistry;
import net.customware.gwt.dispatch.server.Dispatch;
import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import net.customware.gwt.dispatch.server.SimpleDispatch;
import net.customware.gwt.dispatch.server.standard.AbstractStandardDispatchServlet;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.Result;
import org.apache.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import platform.gwt.base.server.exceptions.RemoteDispatchException;
import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.base.server.spring.NavigatorProvider;
import platform.gwt.base.shared.InvalidateException;
import platform.gwt.base.shared.MessageException;
import platform.interop.RemoteLogicsInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.rmi.RemoteException;

public abstract class LogicsDispatchServlet<T extends RemoteLogicsInterface> extends AbstractStandardDispatchServlet {
    protected final static Logger logger = Logger.getLogger(LogicsDispatchServlet.class);

    private boolean useGETForGwtRPC;
    protected Dispatch dispatch;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        useGETForGwtRPC = Boolean.parseBoolean(getInitParameter("useGETForGwtRPC"));

        InstanceActionHandlerRegistry registry = new DefaultActionHandlerRegistry();
        addHandlers(registry);
        dispatch = new SimpleDispatch(registry);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (useGETForGwtRPC && req.getParameter("payload") != null) {
            doPost(req, resp);
        }
    }

    @Override
    protected String readContent(HttpServletRequest request) throws ServletException, IOException {
        if (request.getMethod().equals("POST")) {
            return super.readContent(request);
        } else {
            return request.getParameter("payload");
        }
    }

    @Override
    public Result execute(Action<?> action) throws DispatchException {
        try {
            return dispatch.execute(action);
        } catch (RemoteDispatchException e) {
            getLogicsProvider().invalidate();

            logger.error("Ошибка в LogicsDispatchServlet.execute: ", e.getRemote());
            throw new InvalidateException("Внутренняя ошибка сервера. Попробуйте перезагрузить страницу.");
        } catch (MessageException e) {
            logger.error("Ошибка в LogicsDispatchServlet.execute: ", e);
            throw new MessageException("Внутренняя ошибка сервера: " + e.getMessage());
        } catch (Throwable e) {
            logger.error("Ошибка в LogicsDispatchServlet.execute: ", e);
            throw new MessageException("Внутренняя ошибка сервера.");
        }
    }

    @Override
    protected Dispatch getDispatch() {
        return dispatch;
    }

    public T getLogics() {
        return getLogicsProvider().getLogics();
    }

    public RemoteNavigatorInterface getNavigator() throws RemoteException {
        return getNavigatorProvider().getNavigator();
    }

    public BusinessLogicsProvider<T> getLogicsProvider() {
        return getSpringContext().getBean(BusinessLogicsProvider.class);
    }

    public NavigatorProvider getNavigatorProvider() {
        return getSpringContext().getBean(NavigatorProvider.class);
    }

    //todo: это будет не нужно после интеграции сервлетов в Spring
    protected WebApplicationContext getSpringContext() {
        return WebApplicationContextUtils.getWebApplicationContext(getServletContext());
    }

    public HttpSession getSession() {
        return getThreadLocalRequest().getSession();
    }

    public HttpServletRequest getRequest() {
        return getThreadLocalRequest();
    }

    public HttpServletResponse getResponse() {
        return getThreadLocalResponse();
    }

    public Authentication getAuthentication() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return securityContext.getAuthentication();
    }

    protected abstract void addHandlers(InstanceActionHandlerRegistry registry);
}
