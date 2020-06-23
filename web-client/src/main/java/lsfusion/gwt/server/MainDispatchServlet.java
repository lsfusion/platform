package lsfusion.gwt.server;

import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;
import lsfusion.base.ExceptionUtils;
import lsfusion.gwt.client.base.exception.AuthenticationDispatchException;
import lsfusion.gwt.client.base.exception.RemoteInternalDispatchException;
import lsfusion.gwt.client.base.exception.RemoteMessageDispatchException;
import lsfusion.gwt.client.base.exception.RemoteRetryException;
import lsfusion.gwt.client.controller.remote.action.RequestAction;
import lsfusion.gwt.server.form.handlers.*;
import lsfusion.gwt.server.logics.handlers.GenerateIDHandler;
import lsfusion.gwt.server.navigator.handlers.*;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.http.provider.form.FormProvider;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.http.provider.navigator.NavigatorProvider;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.base.exception.RemoteInternalException;
import lsfusion.interop.base.exception.RemoteMessageException;
import net.customware.gwt.dispatch.server.DefaultActionHandlerRegistry;
import net.customware.gwt.dispatch.server.Dispatch;
import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;
import net.customware.gwt.dispatch.server.SimpleDispatch;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.Result;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.text.ParseException;

// singleton, one for whole application
public class MainDispatchServlet extends net.customware.gwt.dispatch.server.standard.AbstractStandardDispatchServlet implements org.springframework.web.HttpRequestHandler, org.springframework.beans.factory.InitializingBean, org.springframework.beans.factory.BeanNameAware {
    public final static Logger logger = Logger.getLogger(MainDispatchServlet.class);

    protected Dispatch dispatch;

    @Autowired
    private LogicsProvider logicsProvider;
    @Autowired
    private NavigatorProvider navigatorProvider;
    @Autowired
    private FormProvider formProvider;

    @Autowired
    private ServletContext servletContext;

    private boolean useGETForGwtRPC;
    private String rpcPolicyLocation;
    private String beanName;

    protected void addHandlers(InstanceActionHandlerRegistry registry) {
        // global
        registry.addHandler(new CreateNavigatorHandler(this));

        // logics
        registry.addHandler(new GenerateIDHandler(this));

        // navigator
        registry.addHandler(new CloseNavigatorHandler(this));
        registry.addHandler(new ClientPushMessagesHandler(this));
        registry.addHandler(new ContinueNavigatorActionHandler(this));
        registry.addHandler(new ExecuteNavigatorActionHandler(this));
        registry.addHandler(new GetNavigatorInfoHandler(this));
        registry.addHandler(new GetClientSettingsHandler(this));
        registry.addHandler(new LogClientExceptionActionHandler(this));
        registry.addHandler(new GainedFocusHandler(this));
        registry.addHandler(new ThrowInNavigatorActionHandler(this));
        registry.addHandler(new GetRemoteNavigatorActionMessageHandler(this));
        registry.addHandler(new GetRemoteNavigatorActionMessageListHandler(this));
        registry.addHandler(new InterruptNavigatorHandler(this));

        //form
        registry.addHandler(new CalculateSumHandler(this));
        registry.addHandler(new ChangeGroupObjectHandler(this));
        registry.addHandler(new ChangePageSizeHandler(this));
        registry.addHandler(new ChangeModeHandler(this));
        registry.addHandler(new ChangePropertyHandler(this));
        registry.addHandler(new ChangePropertyOrderHandler(this));
        registry.addHandler(new ClearPropertyOrdersHandler(this));
        registry.addHandler(new ClosePressedHandler(this));
        registry.addHandler(new CollapseGroupObjectHandler(this));
        registry.addHandler(new CollapseGroupObjectRecursiveHandler(this));
        registry.addHandler(new ContinueInvocationHandler(this));
        registry.addHandler(new ExecuteEventActionHandler(this));
        registry.addHandler(new ExecuteNotificationHandler(this));
        registry.addHandler(new ExpandGroupObjectHandler(this));
        registry.addHandler(new ExpandGroupObjectRecursiveHandler(this));
        registry.addHandler(new GetInitialFilterPropertyHandler(this));
        registry.addHandler(new GetRemoteActionMessageHandler(this));
        registry.addHandler(new CloseHandler(this));
        registry.addHandler(new GetRemoteActionMessageListHandler(this));
        registry.addHandler(new GetRemoteChangesHandler(this));
        registry.addHandler(new GroupReportHandler(this));
        registry.addHandler(new InterruptHandler(this));
        registry.addHandler(new PasteExternalTableHandler(this));
        registry.addHandler(new PasteSingleCellValueHandler(this));
        registry.addHandler(new RefreshUPHiddenPropsActionHandler(this));
        registry.addHandler(new SaveUserPreferencesActionHandler(this));
        registry.addHandler(new ScrollToEndHandler(this));
        registry.addHandler(new SetRegularFilterHandler(this));
        registry.addHandler(new SetTabVisibleHandler(this));
        registry.addHandler(new SetUserFiltersHandler(this));
        registry.addHandler(new ThrowInInvocationHandler(this));
    }

    public FormProvider getFormProvider() {
        return formProvider;
    }

    public NavigatorProvider getNavigatorProvider() {
        return navigatorProvider;
    }

    public LogicsProvider getLogicsProvider() {
        return logicsProvider;
    }

    public void setUseGETForGwtRPC(boolean useGETForGwtRPC) {
        this.useGETForGwtRPC = useGETForGwtRPC;
    }

    public void setRpcPolicyLocation(String rpcPolicyLocation) {
        this.rpcPolicyLocation = rpcPolicyLocation;
    }

    public void setLogicsProvider(LogicsProvider logicsProvider) {
        this.logicsProvider = logicsProvider;
    }

    @Override
    public void setBeanName(String name) {
        beanName = name;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        FileUtils.createThemedClientImages();
        
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
    protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request, String moduleBaseURL, String strongName) {
        if (rpcPolicyLocation == null) {
            return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
        } else {
            String serializationPolicyFilePath = SerializationPolicyLoader.getSerializationPolicyFileName(rpcPolicyLocation + "/" + strongName);
            // Open the RPC resource file and read its contents.
            InputStream is = getServletContext().getResourceAsStream(serializationPolicyFilePath);
            try {
                if (is != null) {
                    try {
                        return SerializationPolicyLoader.loadFromStream(is, null);
                    } catch (ParseException e) {
                        logger.error("ERROR: Failed to parse the policy file '" + serializationPolicyFilePath + "'", e);
                    } catch (IOException e) {
                        logger.error("ERROR: Could not read the policy file '" + serializationPolicyFilePath + "'", e);
                    }
                } else {
                    logger.error("ERROR: The serialization policy file '" + serializationPolicyFilePath + "' was not found.");
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // Ignore this error
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        service(request, response);
    }

    @Override
    public Result execute(Action<?> action) throws DispatchException {
        try {
            try {
                return dispatch.execute(action);
            } catch (WrappedRemoteDispatchException e) {
                throw e.remoteException;
            }
        } catch (Throwable e) { // mainly AppServerNotAvailableDispatchException, but in theory can be some InvalidSessionException
            DispatchException ed = fromWebServerToWebClient(e);
            
            logException(action, ed);
            
            throw ed;
        }
    }

    // result throwable class should exist on client
    public static DispatchException fromWebServerToWebClient(Throwable e) {
        if(e instanceof DispatchException) // mainly AppServerNotAvailableDispatchException, but in theory can be some InvalidSessionException
            return (DispatchException) e;
        // we need to wrap next two exceptions, otherwise they will be treated like RemoteInternalDispatchException (unknown server exception)
        if(e instanceof AuthenticationException)
            return new AuthenticationDispatchException(e.getMessage());
        if(e instanceof RemoteMessageException)
            return new RemoteMessageDispatchException(e.getMessage());
        if(e instanceof RemoteException && !(ExceptionUtils.getRootCause(e) instanceof ClassNotFoundException)) // when client action goes to web, because there is no classloader like in desktop, we'll get ClassNotFoundException, and we don't want to consider it connection problem
            return new RemoteRetryException(e, e instanceof SessionInvalidatedException ? 3 : ExceptionUtils.getFatalRemoteExceptionCount(e));

        RemoteInternalDispatchException clientException = new RemoteInternalDispatchException(ExceptionUtils.copyMessage(e), RemoteInternalException.getLsfStack(e));
        ExceptionUtils.copyStackTraces(e, clientException);        
        //we do it because of problem with deserialization of exception's stacktrace
        clientException.javaStack = ExceptionUtils.getStackTrace(clientException);
        return clientException;
    }

    private void logException(Action<?> action, DispatchException e) {
        if(e instanceof RemoteRetryException)
            logRemoteRetryException(action, (RemoteRetryException) e);
        else
            logger.error("Error in LogicsAwareDispatchServlet.execute: ", e);
    }

    private void logRemoteRetryException(Action<?> action, RemoteRetryException et) {
        if (!(action instanceof RequestAction) || ((RequestAction) action).logRemoteException()) {
            String actionTry = "";
            if(action instanceof RequestAction) {
                actionTry = "\n" + action + " try: " + ((RequestAction) action).requestTry + ", maxTries: " + et.maxTries;
            }
            logger.error("Error in LogicsAwareDispatchServlet.execute: " + actionTry, et);
        }
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getServletName() {
        return beanName;
    }

    @Override
    protected Dispatch getDispatch() {
        return dispatch;
    }

    public String getSessionInfo() {
        return navigatorProvider.getSessionInfo();
    }

    public HttpServletRequest getRequest() {
        return getThreadLocalRequest();
    }

    @Override
    public String getRequestModuleBasePath() {
        return super.getRequestModuleBasePath();
    }
}
