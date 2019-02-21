package lsfusion.gwt.server;

import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.gwt.server.form.handlers.*;
import lsfusion.gwt.shared.exceptions.*;
import lsfusion.http.provider.form.FormProvider;
import lsfusion.gwt.server.logics.handlers.GenerateIDHandler;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.gwt.server.navigator.handlers.*;
import lsfusion.http.provider.navigator.NavigatorProvider;
import lsfusion.gwt.shared.actions.RequestAction;
import lsfusion.interop.exceptions.AuthenticationException;
import lsfusion.interop.exceptions.RemoteInternalException;
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

import static lsfusion.base.ServerMessages.getString;

// singleton, one for whole application
public class MainDispatchServlet extends net.customware.gwt.dispatch.server.standard.AbstractStandardDispatchServlet implements org.springframework.web.HttpRequestHandler, org.springframework.beans.factory.InitializingBean, org.springframework.beans.factory.BeanNameAware {
    protected final static Logger logger = Logger.getLogger(MainDispatchServlet.class);

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
        registry.addHandler(new SetCurrentFormHandler(this));
        registry.addHandler(new ThrowInNavigatorActionHandler(this));
        registry.addHandler(new GetRemoteNavigatorActionMessageHandler(this));
        registry.addHandler(new GetRemoteNavigatorActionMessageListHandler(this));
        registry.addHandler(new InterruptNavigatorHandler(this));

        //form
        registry.addHandler(new CalculateSumHandler(this));
        registry.addHandler(new ChangeClassViewHandler(this));
        registry.addHandler(new ChangeGroupObjectHandler(this));
        registry.addHandler(new ChangePageSizeHandler(this));
        registry.addHandler(new ChangePropertyHandler(this));
        registry.addHandler(new ChangePropertyOrderHandler(this));
        registry.addHandler(new ClearPropertyOrdersHandler(this));
        registry.addHandler(new ClosePressedHandler(this));
        registry.addHandler(new CollapseGroupObjectHandler(this));
        registry.addHandler(new ContinueInvocationHandler(this));
        registry.addHandler(new CountRecordsHandler(this));
        registry.addHandler(new ExecuteEditActionHandler(this));
        registry.addHandler(new ExecuteNotificationHandler(this));
        registry.addHandler(new ExpandGroupObjectHandler(this));
        registry.addHandler(new FormHiddenHandler(this));
        registry.addHandler(new GetInitialFilterPropertyHandler(this));
        registry.addHandler(new GetRemoteActionMessageHandler(this));
        registry.addHandler(new GetRemoteActionMessageListHandler(this));
        registry.addHandler(new GetRemoteChangesHandler(this));
        registry.addHandler(new GroupReportHandler(this));
        registry.addHandler(new InterruptHandler(this));
        registry.addHandler(new OkPressedHandler(this));
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
        } catch (DispatchException e) { // mainly AppServerNotAvailableException, but in theory can be some InvalidSessionException 
            throw e; // just rethrow
        } catch (AuthenticationException e) { // we need to wrap this exception, otherwise it will be treated like RemoteInternalDispatchException (unknown server exception)
            throw new AuthenticationDispatchException(e.getMessage());
        } catch (RemoteException e) {
            if(ExceptionUtils.getRootCause(e) instanceof ClassNotFoundException) // when client action goes to web, because there is no classloader like in desktop, we'll get ClassNotFoundException, and we don't want to consider it connection problem
                throw handleNotDispatch(e);
            
            // connection problem, need to retry request
            RemoteRetryException et = new RemoteRetryException(e.getMessage(), e, ExceptionUtils.getFatalRemoteExceptionCount(e));
            logRemoteRetryException(action, et);
            throw et;
        } catch (Throwable e) { // all other exceptions 
            throw handleNotDispatch(e);
        }
    }
    
    // wrapping RemoteServerException (app) + web server exception -> RemoteInternalDispatchException
    private RemoteInternalDispatchException handleNotDispatch(Throwable e) {
        logNotDispatchException(e);

        Pair<String, String> allStacks = RemoteInternalException.getActualStacks(e);
        return new RemoteInternalDispatchException(e, allStacks.first, allStacks.second); 
    }

    private void logNotDispatchException(Throwable e) {
        logger.error("Error in LogicsAwareDispatchServlet.execute: ", e);
    }

    public void logRemoteRetryException(Action<?> action, RemoteRetryException et) {
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
}
