package lsfusion.server.physics.admin.authentication.controller.remote;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.file.FileData;
import lsfusion.base.file.NamedFileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.connection.*;
import lsfusion.interop.session.*;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.controller.remote.RemoteRequestObject;
import lsfusion.server.base.controller.thread.SyncType;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.LSFStatusException;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.form.interactive.action.async.PushAsyncResult;
import lsfusion.server.logics.form.interactive.changed.FormChanges;
import lsfusion.server.logics.navigator.controller.env.*;
import lsfusion.server.logics.navigator.controller.remote.RemoteNavigator;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.log.LogInfo;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.external.to.CallHTTPAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;

import static lsfusion.base.BaseUtils.getNotNullStringArray;
import static lsfusion.base.BaseUtils.nvl;

public abstract class RemoteConnection extends RemoteRequestObject implements RemoteConnectionInterface {

    protected SQLSession sql;

    public LogicsInstance logicsInstance;
    protected BusinessLogics businessLogics;
    protected DBManager dbManager;
    public String remoteAddress;

    protected DataObject computer;
    protected SecurityManager securityManager;
    protected AuthenticationToken token;
    protected DataObject user;
    protected String userName;
    protected String computerName;
    protected boolean allowExcessAllocatedBytes;
    protected Locale locale;
    protected LocalePreferences localePreferences;
    public Long userRole;
    protected Integer transactionTimeout;

    protected String userRoles;

    public RemoteConnection(int port, String sID, LogicsInstance logicsInstance, AuthenticationToken token, SessionInfo sessionInfo, ExecutionStack upStack) throws RemoteException, SQLException, SQLHandledException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        super(port, upStack, sID, SyncType.NOSYNC);

        initContext(logicsInstance);

        initConnectionContext(token, sessionInfo.connectionInfo, upStack);
    }
    
    protected abstract FormController createFormController();
    protected abstract ChangesController createChangesController();    

    protected DataSession createSession() throws SQLException {
        return dbManager.createSession(sql, new WeakUserController(this), createFormController(), new WeakTimeoutController(this), createChangesController(), new WeakLocaleController(this), dbManager.getIsServerRestartingController(), null);
    }
    protected LogInfo logInfo;

    protected void initContext(LogicsInstance logicsInstance) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLHandledException {
        businessLogics = logicsInstance.getBusinessLogics();
        dbManager = logicsInstance.getDbManager();
        securityManager = logicsInstance.getSecurityManager();
        this.logicsInstance = logicsInstance;

        this.sql = dbManager.createSQL(new WeakSQLSessionContextProvider(this));
    }

    // it's important to have all props lazy to make RemoteSession be initialized faster
    public void initConnectionContext(AuthenticationToken token, ConnectionInfo connectionInfo, ExecutionStack stack) throws SQLException, SQLHandledException {
        try(ExecSession session = getExecSession()) {
            initUser(token, connectionInfo.userInfo, session.dataSession, stack);

            initComputer(stack, connectionInfo.computerInfo, session.dataSession);
        }
    }

    private void initUser(AuthenticationToken token, UserInfo userInfo, DataSession session, ExecutionStack stack) throws SQLException, SQLHandledException {
        this.token = token;
        user = securityManager.getUser(securityManager.parseToken(token), session);

        saveUserContext(userInfo, stack, session);

        initUserContext(session);
    }

    private void initComputer(ExecutionStack stack, ComputerInfo computerInfo, DataSession session) {
        String hostName = computerInfo.hostName;

        remoteAddress = computerInfo.hostAddress;
        computerName = hostName;
        computer = dbManager.getComputer(hostName, session, stack); // can apply session

        logInfo = null;
    }

    protected void saveUserContext(UserInfo userInfo, ExecutionStack stack, DataSession session) throws SQLException, SQLHandledException {
        TimeZone timeZone = userInfo.timeZone;
        businessLogics.authenticationLM.clientTimeZone.change(timeZone != null ? timeZone.getID() : null, session, user);
        businessLogics.authenticationLM.clientLanguage.change(userInfo.language, session, user);
        businessLogics.authenticationLM.clientCountry.change(userInfo.country, session, user);
        businessLogics.authenticationLM.clientDateFormat.change(userInfo.dateFormat, session, user);
        businessLogics.authenticationLM.clientTimeFormat.change(userInfo.timeFormat, session, user);
        session.applyException(businessLogics, stack);
    }

    protected void initUserContext(DataSession session) throws SQLException, SQLHandledException {
        userName = (String) businessLogics.authenticationLM.logNameCustomUser.read(session, user);
        allowExcessAllocatedBytes = businessLogics.serviceLM.allowExcessAllocatedBytes.read(session, user) != null;
        userRoles = (String) businessLogics.securityLM.userRolesUser.read(session, user);

        locale = LocalePreferences.getLocale(
                    (String) businessLogics.authenticationLM.language.read(session, user),
                    (String) businessLogics.authenticationLM.country.read(session, user));
        localePreferences = new LocalePreferences(locale,
                (String) businessLogics.authenticationLM.timeZone.read(session, user),
                (Integer) businessLogics.authenticationLM.twoDigitYearStart.read(session, user),
                (String) businessLogics.authenticationLM.dateFormat.read(session, user),
                (String) businessLogics.authenticationLM.timeFormat.read(session, user));

        userRole = (Long) businessLogics.securityLM.firstRoleUser.read(session, user);
        transactionTimeout = (Integer) businessLogics.serviceLM.transactTimeoutUser.read(session, user);

        logInfo = null;
    }

    protected static class WeakUserController implements UserController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteConnection> weakThis;

        public WeakUserController(RemoteConnection connection) {
            this.weakThis = new WeakReference<>(connection);
        }

        public boolean changeCurrentUser(DataObject user, ExecutionStack stack) throws SQLException, SQLHandledException {
            RemoteConnection remoteConnection = weakThis.get();
            return remoteConnection != null && remoteConnection.changeCurrentUser(user, stack);
        }

        public Long getCurrentUserRole() {
            RemoteConnection remoteConnection = weakThis.get();
            return remoteConnection == null ? null : remoteConnection.userRole;
        }
    }

    protected static class WeakLocaleController implements LocaleController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteConnection> weakThis;

        public WeakLocaleController(RemoteConnection connection) {
            this.weakThis = new WeakReference<>(connection);
        }

        @Override
        public Locale getLocale() {
            RemoteConnection remoteConnection = weakThis.get();
            if(remoteConnection != null)
                return remoteConnection.getLocale();
            return Locale.getDefault();
        }
    }

    protected static class WeakTimeoutController implements TimeoutController { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteConnection> weakThis;

        public WeakTimeoutController(RemoteConnection connection) {
            this.weakThis = new WeakReference<>(connection);
        }

        public int getTransactionTimeout() {
            RemoteConnection remoteConnection = weakThis.get();
            if(remoteConnection == null)
                return 0;
            return remoteConnection.getTransactionTimeout();
        }
    }

    protected static class WeakSQLSessionContextProvider implements SQLSessionContextProvider { // чтобы помочь сборщику мусора и устранить цикл
        WeakReference<RemoteConnection> weakThis;

        public WeakSQLSessionContextProvider(RemoteConnection dbManager) {
            this.weakThis = new WeakReference<>(dbManager);
        }

        @Override
        public Long getCurrentUser() {
            final RemoteConnection wThis = weakThis.get();
            if(wThis == null) // используется в мониторе процессов
                return null;
            return wThis.getCurrentUser();
        }

        @Override
        public String getCurrentAuthToken() {
            final RemoteConnection wThis = weakThis.get();
            if(wThis == null)
                return null;
            return wThis.getCurrentAuthToken();
        }

        public LogInfo getLogInfo() {
            final RemoteConnection wThis = weakThis.get();
            if(wThis == null) // используется в мониторе процессов
                return null;
            return wThis.getLogInfo();
        }

        @Override
        public Long getCurrentComputer() {
            final RemoteConnection wThis = weakThis.get();
            if(wThis == null)
                return null;
            return wThis.getCurrentComputer();
        }

        @Override
        public Long getCurrentConnection() {
            final RemoteConnection wThis = weakThis.get();
            if(wThis == null)
                return null;
            return wThis.getConnectionId();
        }

        @Override
        public LocalePreferences getLocalePreferences() {
            final RemoteConnection wThis = weakThis.get();
            return wThis == null ? null : wThis.getLocalePreferences();
        }
    }

    public boolean changeCurrentUser(DataObject user, ExecutionStack stack) throws SQLException, SQLHandledException {
        this.user = user;
        try(DataSession session = createSession()) {
            initUserContext(session);
        }
        return true;
    }

    @ManualLazy
    public LogInfo getLogInfo() {
        if(logInfo == null)
            logInfo = new LogInfo(allowExcessAllocatedBytes, userName, userRoles, computerName, remoteAddress);
        return logInfo;
    }

    public int getTransactionTimeout() {
        return transactionTimeout != null ? transactionTimeout : 0;
    }

    public Locale getLocale() {
        return locale;
    }

    public Long getCurrentUser() {
        return user != null ? (Long)user.object : null;
    }

    protected String getCurrentAuthToken() {
        return token != null ? token.string : null;
    }

    public Long getCurrentComputer() {
        return computer != null ? (Long)computer.object : null;
    }

    protected abstract Long getConnectionId();

    public LocalePreferences getLocalePreferences() {
        return localePreferences;
    }

    @Override
    protected void onClose() {
        super.onClose();

        try {
            if(!isLocal()) // localClose are not done through remote calls
                ThreadLocalContext.assureRmi(RemoteConnection.this);

            sql.close();
        } catch (Throwable t) {
            ServerLoggers.sqlSuppLog(t);
        }
    }

    @Override
    public ExternalResponse exec(String actionName, ExternalRequest request) {
            return logFromExternalSystemRequest(() -> {
                if(actionName != null) {
                    LA action;
                    String findActionName = actionName;
                    String actionPathInfo = "";
                    while(true) { // we're doing greedy search for all subpathes to find appropriate "endpoint" action
                        if ((action = businessLogics.findActionByCompoundName(findActionName.replace('/', '_'))) != null)
                            break;

                        int lastSlash = findActionName.lastIndexOf('/'); // if it is url
                        if (lastSlash < 0)
                            break;
                        findActionName = findActionName.substring(0, lastSlash);
                        actionPathInfo = actionName.substring(lastSlash + 1);
                    }
                    if (action != null) {
                        return executeExternal(action, actionName, actionPathInfo, false, request);
                    } else {
                        throw new LSFStatusException(String.format("Action %s was not found", actionName), 404);
                    }
                } else {
                    throw new RuntimeException("Action was not specified");
                }
            }, true, actionName, request);
    }

    @Override
    public ExternalResponse eval(boolean action, ExternalRequest.Param paramScript, ExternalRequest request) {
        String script = parseScript(paramScript);

        return logFromExternalSystemRequest(() -> {
            if (script != null) {
                LA<?> runAction = businessLogics.evaluateRun(script, action);
                if(runAction != null) {
                    return executeExternal(runAction, paramScript.value, null, true, request);
                } else {
                    throw new RuntimeException("Action with name 'run' was not found");
                }
            } else {
                throw new RuntimeException("Eval script was not found");
            }
        }, false, script, request);
    }

    private String parseScript(ExternalRequest.Param paramScript) {
        try {
            return paramScript != null ? StringClass.text.parseHTTP(paramScript) : null;
        } catch (ParseException e) {
            return null;
        }
    }

    private ExternalResponse executeExternal(LA<?> property, Object actionParam, String actionPathInfo, boolean script, ExternalRequest request) {
        checkEnableApi(property, actionParam, script, request);

        RemoteNavigator.Notification runnable = new RemoteNavigator.Notification() {
            @Override
            public void run(ExecutionEnvironment env, ExecutionStack stack, PushAsyncResult asyncResult) {
                try {
                    RemoteConnection.this.executeExternal(property, request, actionPathInfo, env, stack);
                } catch (Throwable t) {
                    throw Throwables.propagate(t);
                }
            }

            @Override
            protected Action<?> getAction() {
                return property.action;
            }
        };

        if(request.needNotificationId)
            return new ResultExternalResponse(new ExternalRequest.Result[]{formatReturnValue(RemoteNavigator.pushGlobalNotification(runnable), IntegerClass.instance, null, null)}, new String[0], new String[0], new String[0], new String[0], HttpServletResponse.SC_OK);
        else if(property.action.hasFlow(ChangeFlowType.INTERACTIVEWAIT)) {

            int mode = Settings.get().getExternalUINotificationMode();

            boolean serverNotification = mode >= 1;
            if(serverNotification) {
                boolean pendNotification = mode == 2;

                boolean foundNavigator = true;
                if (this instanceof RemoteNavigator)
                    ((RemoteNavigator) this).pushNotification(runnable);
                else
                    foundNavigator = logicsInstance.getNavigatorsManager().pushNotificationSession(request.sessionId, runnable, pendNotification);

                if (foundNavigator)
                    return new RedirectExternalResponse("/push-notification", null);
                else
                    return new RedirectExternalResponse("/", pendNotification ? null : RemoteNavigator.pushGlobalNotification(runnable));
            } else
                return new RedirectExternalResponse("/push-notification", RemoteNavigator.pushGlobalNotification(runnable));
        } else {
            try {
                try (ExecSession execSession = getExecSession()) {
                    DataSession dataSession = execSession.dataSession;

                    runnable.run(dataSession, getStack(), null);

                    return readResult(request.returnNames, request.returnMultiType, property.action, dataSession);
                }
            } catch (SQLException | SQLHandledException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private void executeExternal(LA<?> property, ExternalRequest request, String actionPathInfo, ExecutionEnvironment env, ExecutionStack stack) throws SQLException, SQLHandledException, ParseException {
        writeRequestInfo(env, property.action, request, actionPathInfo);

        property.execute(env, stack, CallHTTPAction.getParams(env.getSession(), property, request.params));
    }

    protected AuthenticationException authException;

    private void checkEnableApi(LA<?> property, Object actionParam, boolean script, ExternalRequest request) {
        boolean forceAPI = false;
        String annotation = property.action.annotation;
        if(annotation != null) {
            if(annotation.equals("noauth"))
                return;
            forceAPI = annotation.equals("api");
        }

        if(request.signature != null && securityManager.verifyData(ExternalUtils.generate(actionParam, script, request.getImplicitParamValues()), request.signature))
            return;

        if(authException != null)
            throw authException;

        checkEnableApi(token.isAnonymous(), forceAPI);
    }

    private static void checkEnableApi(boolean anonymous, boolean forceAPI) {
        byte enableApi = Settings.get().getEnableAPI();
        if(enableApi == 0) {
            if(forceAPI)
                enableApi = 1;
            else
                throw new RuntimeException("Api is disabled. It can be enabled by using setting enableAPI.");
        }

        if(anonymous && enableApi == 1)
            throw new AuthenticationException();
    }

    public void writeRequestInfo(ExecutionEnvironment env, Action<?> action, ExternalRequest request, String actionPathInfo) throws SQLException, SQLHandledException {
        DataSession session = env.getSession();
        if (action.uses(businessLogics.LM.headers.property)) {
            CallHTTPAction.writePropertyValues(session, env, businessLogics.LM.headers, getNotNullStringArray(request.headerNames), getNotNullStringArray(request.headerValues));
        }
        if (action.uses(businessLogics.LM.cookies.property)) {
            CallHTTPAction.writePropertyValues(session, env, businessLogics.LM.cookies, getNotNullStringArray(request.cookieNames), getNotNullStringArray(request.cookieValues));
        }
        if (action.uses(businessLogics.LM.query.property)) {
            businessLogics.LM.query.change(request.query, session);
        }
        if (action.uses(businessLogics.LM.params.property)) {
            MExclMap<ImList<Object>, String> mParams = MapFact.mExclMap();
            Map<String, Integer> paramIndexes = new HashMap<>();
            for (ExternalRequest.Param param : request.params) {
                String paramName = param.name;
                Object paramValue = param.value;

                if(paramValue instanceof String) {
                    Integer paramIndex = paramIndexes.get(paramName);
                    if (paramIndex == null)
                        paramIndex = 0;
                    paramIndexes.put(paramName, paramIndex + 1);

                    mParams.exclAdd(ListFact.toList(paramName, (Object) paramIndex), (String) paramValue);
                }
            }
            CallHTTPAction.writePropertyValues(session, env, businessLogics.LM.params, mParams.immutable());
        }
        if (action.uses(businessLogics.LM.fileParams.property)) {
            MExclMap<ImList<Object>, NamedFileData> mParams = MapFact.mExclMap();
            Map<String, Integer> paramIndexes = new HashMap<>();
            for (ExternalRequest.Param param : request.params) {
                String paramName = param.name;
                Object paramValue = param.value;

                if(paramValue instanceof FileData) {
                    Integer paramIndex = paramIndexes.get(paramName);
                    if (paramIndex == null)
                        paramIndex = 0;
                    paramIndexes.put(paramName, paramIndex + 1);

                    mParams.exclAdd(ListFact.toList(paramName, (Object) paramIndex), ExternalRequest.getNamedFile((FileData) paramValue, param.fileName));
                }
            }
            CallHTTPAction.writePropertyValues(session, env, businessLogics.LM.fileParams, mParams.immutable());
        }
        if (action.uses(businessLogics.LM.actionPathInfo.property)) {
            businessLogics.LM.actionPathInfo.change(actionPathInfo, session);
        }
        if (action.uses(businessLogics.LM.contentType.property)) {
            businessLogics.LM.contentType.change(request.contentType, session);
        }
        if (action.uses(businessLogics.LM.body.property)) {
            byte[] body = request.body;
            businessLogics.LM.body.change(body != null ? new RawFileData(body) : null, session);
        }
        if (action.uses(businessLogics.LM.appHost.property)) {
            businessLogics.LM.appHost.change(request.appHost, session);
        }
        if (action.uses(businessLogics.LM.appPort.property)) {
            businessLogics.LM.appPort.change(request.appPort, session);
        }
        if (action.uses(businessLogics.LM.exportName.property)) {
            businessLogics.LM.exportName.change(request.exportName, session);
        }
        if (action.uses(businessLogics.LM.scheme.property)) {
            businessLogics.LM.scheme.change(request.scheme, session);
        }
        if (action.uses(businessLogics.LM.method.property)) {
            businessLogics.LM.method.change(request.method, session);
        }
        if (action.uses(businessLogics.LM.webHost.property)) {
            businessLogics.LM.webHost.change(request.webHost, session);
        }
        if (action.uses(businessLogics.LM.webPort.property)) {
            businessLogics.LM.webPort.change(request.webPort, session);
        }
        if (action.uses(businessLogics.LM.contextPath.property)) {
            businessLogics.LM.contextPath.change(request.contextPath, session);
        }
        if (action.uses(businessLogics.LM.servletPath.property)) {
            businessLogics.LM.servletPath.change(request.servletPath, session);
        }
        if (action.uses(businessLogics.LM.pathInfo.property)) {
            businessLogics.LM.pathInfo.change(request.pathInfo, session);
        }
    }

    private ExternalResponse readResult(String[] returnNames, String returnMultiType, Action<?> property, DataSession dataSession) throws SQLException, SQLHandledException {

        ImOrderMap<String, String> headers = CallHTTPAction.readPropertyValues(dataSession, businessLogics.LM.headersTo).toOrderMap();
        String[] headerNames = headers.keyOrderSet().toArray(new String[headers.size()]);
        String[] headerValues = headers.valuesList().toArray(new String[headers.size()]);
        ImOrderMap<String, String> cookies = CallHTTPAction.readPropertyValues(dataSession, businessLogics.LM.cookiesTo).toOrderMap();
        String[] cookieNames = cookies.keyOrderSet().toArray(new String[cookies.size()]);
        String[] cookieValues = cookies.valuesList().toArray(new String[cookies.size()]);

        Integer statusHttp = (Integer) businessLogics.LM.statusHttpTo.read(dataSession);

        ExternalUtils.ResponseType responseType = ExternalUtils.getResponseType(returnMultiType, headerNames, headerValues);
        Charset charset = responseType.charset;

        List<ExternalRequest.Result> returns = new ArrayList<>();

        LP[] returnProps;
        if (returnNames.length > 0) {
            returnProps = new LP[returnNames.length];
            for (int i = 0; i < returnNames.length; i++) {
                String returnName = returnNames[i];
                LP returnProperty = businessLogics.findPropertyByCompoundName(returnName);
                if (returnProperty == null)
                    throw new RuntimeException(String.format("Return property %s was not found", returnName));
                returnProps[i] = returnProperty;
            }
            for (LP<?> returnProp : returnProps)
                returns.add(formatReturnValue(returnProp.read(dataSession), returnProp.property, charset, returnProps.length > 1 ? returnProp.property.getName() : null));
        } else {
            Result<SessionDataProperty> resultProp = new Result<>();
            ObjectValue objectValue = businessLogics.LM.getExportValueProperty().readFirstNotNull(dataSession, resultProp, property);
            returns.add(formatReturnValue(objectValue.getValue(), resultProp.result, charset, null));
        }

        return new ResultExternalResponse(returns.toArray(new ExternalRequest.Result[0]), headerNames, headerValues, cookieNames, cookieValues, nvl(statusHttp, HttpServletResponse.SC_OK));
    }

    private ExternalRequest.Result formatReturnValue(Object returnValue, Type type, Charset charset, String paramName) {
        // response requires filename if the file is returned
        return type.formatHTTP(returnValue, charset, true).convertFileValue(paramName, value -> FormChanges.convertFileValue(value, getContext().getConnectionContext()));
    }
    private ExternalRequest.Result formatReturnValue(Object returnValue, Property returnProperty, Charset charset, String paramName) {
        return formatReturnValue(returnValue, returnProperty.getType(), charset, paramName);
    }

    protected abstract ExecSession getExecSession() throws SQLException;

    private ExternalResponse logFromExternalSystemRequest(Callable<ExternalResponse> responseCallable, boolean exec, String action, ExternalRequest request) {
        String requestLogMessage = Settings.get().isLogFromExternalSystemRequests() ? getExternalSystemRequestsLog(logInfo, request.servletPath, request.method,
                "\tREQUEST_QUERY: " + request.query + "\n" + "\t" + (exec ? "ACTION" : "SCRIPT") + ":\n\t\t " + action) : null;
        boolean successfulResponse = false;
        try {
            ExternalResponse execResult = responseCallable.call();

            successfulResponse = successfulResponse(execResult.getStatusHttp());

            if (requestLogMessage != null && Settings.get().isLogFromExternalSystemRequestsDetail()) {
                ExternalUtils.ExternalResponse externalResponse = ExternalUtils.getExternalResponse(execResult, request.returnMultiType, null, value -> logicsInstance.getRmiManager().convertFileValue(request, value));

                if(externalResponse instanceof ExternalUtils.ResultExternalResponse) {
                    ExternalUtils.ResultExternalResponse result = (ExternalUtils.ResultExternalResponse) externalResponse;

                    requestLogMessage += getExternalSystemRequestsLogDetail(BaseUtils.toStringMap(request.headerNames, request.headerValues),
                            BaseUtils.toStringMap(request.cookieNames, request.cookieValues),
                            request.body != null ? new String(request.body, ExternalUtils.getLoggingCharsetFromContentType(request.contentType)) : null,
                            null,
                            BaseUtils.toStringMap(result.headerNames, result.headerValues),
                            BaseUtils.toStringMap(result.cookieNames, result.cookieValues),
                            String.valueOf(result.statusHttp),
                            "\tOBJECTS:\n\t\t" + result.response);
                }
            }
            return execResult;
        } catch (Throwable t) {
            if (requestLogMessage != null)
                requestLogMessage += "\n\tERROR: " + t.getMessage() + "\n";

            throw Throwables.propagate(t);
        } finally {
            logExternalSystemRequest(ServerLoggers.httpFromExternalSystemRequestsLogger, requestLogMessage, successfulResponse);
        }
    }

    public class ExecSession implements AutoCloseable {

        public DataSession dataSession;

        public ExecSession(DataSession dataSession) {
           this.dataSession = dataSession;
        }

        public void close() throws SQLException {
        }
    }

    public static String getExternalSystemRequestsLog (LogInfo logInfo, String path, String method, String extraValue) {
        return "\nREQUEST:\n" +
                (logInfo != null ? "\tREQUEST_USER_INFO: " + logInfo + "\n" : "") +
                "\tREQUEST_PATH: " + path + "\n" +
                "\tREQUEST_METHOD: " + method + "\n" +
                (extraValue != null ? "\n" + extraValue + "\n" : "");
    }

    public static String getExternalSystemRequestsLogDetail (Map<String, String> requestHeaders, Map<String, String> requestCookies, String requestBody,
                                                             String requestExtraValue, Map<String, String> responseHeaders, Map<String, String> responseCookies,
                                                             String responseStatus, String responseExtraValue) {
        return getLogMapValues("REQUEST_HEADERS:", requestHeaders) + "\n" +
                getLogMapValues("REQUEST_COOKIES:", requestCookies) + "\n" +
                (requestBody != null ? "\tBODY:\n\t\t" + requestBody + "\n" : "") +
                (requestExtraValue != null ? requestExtraValue + "\n" : "") +
                "RESPONSE:\n" +
                getLogMapValues("RESPONSE_HEADERS:", responseHeaders) + "\n" +
                getLogMapValues("RESPONSE_COOKIES:", responseCookies) + "\n" +
                "\tRESPONSE_STATUS_HTTP: " + responseStatus + "\n" +
                (responseExtraValue != null ? responseExtraValue : "");
    }

    private static String getLogMapValues(String caption, Map<String, String> map) {
        return "\t" + caption + "\n\t\t" + StringUtils.join(map.entrySet().iterator(), "\n\t\t");
    }

    public static boolean successfulResponse(int responseStatusCode) {
        return responseStatusCode >= 200 && responseStatusCode < 300;
    }

    public static void logExternalSystemRequest (Logger logger, String message, boolean successfulResponse) {
        if (message != null) {
            if (successfulResponse)
                logger.info(message);
            else
                logger.error(message);
        }
    }

}
