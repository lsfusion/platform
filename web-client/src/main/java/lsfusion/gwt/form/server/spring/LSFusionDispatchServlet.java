package lsfusion.gwt.form.server.spring;

import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;
import lsfusion.gwt.base.server.exceptions.RemoteRetryException;
import lsfusion.gwt.base.shared.MessageException;
import lsfusion.gwt.base.shared.RetryException;
import lsfusion.gwt.base.shared.actions.RequestAction;
import lsfusion.gwt.form.server.form.handlers.*;
import lsfusion.gwt.form.server.form.spring.FormProvider;
import lsfusion.gwt.form.server.logics.handlers.CreateNavigatorActionHandler;
import lsfusion.gwt.form.server.logics.handlers.GenerateIDHandler;
import lsfusion.gwt.form.server.logics.spring.LogicsProvider;
import lsfusion.gwt.form.server.logics.spring.LogicsSessionObject;
import lsfusion.gwt.form.server.navigator.handlers.*;
import lsfusion.gwt.form.server.navigator.spring.NavigatorProvider;
import lsfusion.gwt.form.shared.view.GLogics;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.exceptions.RemoteInternalException;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.ClientCallBackInterface;
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
public class LSFusionDispatchServlet extends net.customware.gwt.dispatch.server.standard.AbstractStandardDispatchServlet implements org.springframework.web.HttpRequestHandler, org.springframework.beans.factory.InitializingBean, org.springframework.beans.factory.BeanNameAware {
    protected final static Logger logger = Logger.getLogger(LSFusionDispatchServlet.class);

    protected Dispatch dispatch;

    @Autowired
    private NavigatorProvider navigatorProvider;
    @Autowired
    private FormProvider formProvider;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private LogicsProvider<RemoteLogicsInterface> blProvider;

    private boolean useGETForGwtRPC;
    private String rpcPolicyLocation;
    private String beanName;

    protected void addHandlers(InstanceActionHandlerRegistry registry) {
        // logics
        registry.addHandler(new CreateNavigatorActionHandler(this));
        registry.addHandler(new GenerateIDHandler(this));

        // navigator
        registry.addHandler(new CloseNavigatorHandler(this));
        registry.addHandler(new ClientPushMessagesHandler(this));
        registry.addHandler(new ContinueNavigatorActionHandler(this));
        registry.addHandler(new ExecuteNavigatorActionHandler(this));
        registry.addHandler(new ForbidDuplicateFormsHandler(this));
        registry.addHandler(new GetNavigatorInfoHandler(this));
        registry.addHandler(new GetLocaleHandler(this));
        registry.addHandler(new GetClientSettingsHandler(this));
        registry.addHandler(new IsConfigurationAccessAllowedHandler(this));
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

    public void setUseGETForGwtRPC(boolean useGETForGwtRPC) {
        this.useGETForGwtRPC = useGETForGwtRPC;
    }

    public void setRpcPolicyLocation(String rpcPolicyLocation) {
        this.rpcPolicyLocation = rpcPolicyLocation;
    }

    public void setBlProvider(LogicsProvider<RemoteLogicsInterface> blProvider) {
        this.blProvider = blProvider;
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
            return dispatch.execute(action);
        } catch (RemoteRetryException e) {
            String actionTry = "";
            if(action instanceof RequestAction) {
                actionTry = "\n" + action + " try: " + ((RequestAction) action).requestTry + ", maxTries: " + e.maxTries;
            }
            if (!(action instanceof RequestAction) || ((RequestAction) action).logRemoteException()) {
                logger.error("Ошибка в LogicsAwareDispatchServlet.execute: " + actionTry, e);
            }
            throw new RetryException(e.getMessage(), e.maxTries);
        } catch (MessageException e) {
            logger.error("Ошибка в LogicsAwareDispatchServlet.execute: ", e);
            throw new MessageException("Внутренняя ошибка сервера: " + e.getMessage());
        } catch (RemoteInternalException e) {
            logger.error("Ошибка в LogicsAwareDispatchServlet.execute: ", e);
            throw new MessageException("Внутренняя ошибка сервера.", e, e.lsfStack);
        } catch (Throwable e) {
            logger.error("Ошибка в LogicsAwareDispatchServlet.execute: ", e);
            throw new MessageException("Внутренняя ошибка сервера.", e);
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

    public RemoteLogicsInterface getLogics() throws RemoteException {
        return blProvider.getLogics();
    }

    public LogicsProvider<RemoteLogicsInterface> getBLProvider() {
        return blProvider;
    }

    public void tabClosed(String tabSID) throws RemoteException {
        formProvider.removeFormSessionObjects(tabSID);

        if (navigatorProvider.tabClosed(tabSID)) {
            invalidate();
        }
    }

    public String getSessionInfo() {
        return navigatorProvider.getSessionInfo();
    }

    public void invalidate() throws RemoteException {
        try {
            blProvider.invalidate();
        } finally {
            try {
                navigatorProvider.getNavigator().close();
            } finally {
                navigatorProvider.invalidate();
            }
        }
    }

    public GLogics lookupLogics(String host, int port, LogicsProvider logicsProvider) {
        logicsProvider.add
    }
    public LogicsSessionObject getLogicsSessionObject(String logicsSessionID) {
        dfdf
    }

    public HttpServletRequest getRequest() {
        return getThreadLocalRequest();
    }
}
