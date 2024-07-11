package lsfusion.server.physics.admin.authentication.controller.remote;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.connection.RemoteConnectionInterface;
import lsfusion.interop.session.*;
import lsfusion.server.base.controller.remote.RemoteRequestObject;
import lsfusion.server.base.controller.thread.SyncType;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
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
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.form.interactive.action.async.PushAsyncResult;
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
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;
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

    protected DataObject computer;

    protected AuthenticationToken authToken;
    protected DataObject user;
    protected LogInfo logInfo;
    protected Locale locale;
    protected LocalePreferences localePreferences;
    public Long userRole;
    protected Integer transactionTimeout;

    public String sessionId;

    public RemoteConnection(int port, String sID, ExecutionStack upStack) throws RemoteException {
        super(port, upStack, sID, SyncType.NOSYNC);
    }
    
    protected abstract FormController createFormController();
    protected abstract ChangesController createChangesController();    

    protected DataSession createSession() throws SQLException {
        return dbManager.createSession(sql, new WeakUserController(this), createFormController(), new WeakTimeoutController(this), createChangesController(), new WeakLocaleController(this), dbManager.getIsServerRestartingController(), null);
    }

    protected void initContext(LogicsInstance logicsInstance, AuthenticationToken token, SessionInfo connectionInfo, ExecutionStack stack) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLHandledException {
        this.businessLogics = logicsInstance.getBusinessLogics();
        this.dbManager = logicsInstance.getDbManager();
        this.sql = dbManager.createSQL(new WeakSQLSessionContextProvider(this));
        
        this.logicsInstance = logicsInstance;

        sessionId = connectionInfo.externalRequest.sessionId;

        try(DataSession session = createSession()) {
            SecurityManager securityManager = logicsInstance.getSecurityManager();
            initUser(securityManager, token, session);

            String hostName = connectionInfo.hostName;
            computer = dbManager.getComputer(hostName, session, stack); // can apply session

            initUserContext(hostName, connectionInfo.hostAddress, connectionInfo.language, connectionInfo.country, connectionInfo.timeZone, connectionInfo.dateFormat, connectionInfo.timeFormat, connectionInfo.clientColorTheme, stack, session);
        }
    }

    protected void initUser(SecurityManager securityManager, AuthenticationToken token, DataSession session) throws SQLException, SQLHandledException {
        String login = securityManager.parseToken(token);
        authToken = token;
        if(login != null) {
            user = securityManager.readUser(login, session);
            if(user == null) {
                throw new AuthenticationException(String.format("User with login %s not found", login));
            }
        } else {
            user = securityManager.getDefaultLoginUser();
        }
    }

    // in theory its possible to cache all this
    // locale + log info
    protected void initUserContext(String hostName, String remoteAddress, String clientLanguage, String clientCountry, TimeZone clientTimeZone, String clientDateFormat, String clientTimeFormat,
                                   String clientColorTheme, ExecutionStack stack, DataSession session) throws SQLException, SQLHandledException {
        logInfo = readLogInfo(session, user, businessLogics, hostName, remoteAddress);
        locale = readLocale(session, user, businessLogics, clientLanguage, clientCountry, stack);
        userRole = (Long) businessLogics.securityLM.firstRoleUser.read(session, user);
        transactionTimeout = (Integer) businessLogics.serviceLM.transactTimeoutUser.read(session, user);
    }

    public boolean changeCurrentUser(DataObject user, ExecutionStack stack) throws SQLException, SQLHandledException {
        this.user = user;
        try(DataSession session = createSession()) {
            initUserContext(logInfo.hostnameComputer, logInfo.remoteAddress, null, null, null, null, null, null, stack, session);
        }
        return true;
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

    public LogInfo getLogInfo() {
        return logInfo;
    }

    public int getTransactionTimeout() {
        return transactionTimeout != null ? transactionTimeout : 0;
    }

    public static Locale readLocale(DataSession session, DataObject user, BusinessLogics businessLogics, String clientLanguage, String clientCountry, ExecutionStack stack) throws SQLException, SQLHandledException {
        saveClientLanguage(session, user, businessLogics, clientLanguage, clientCountry, stack);

        String language = (String) businessLogics.authenticationLM.language.read(session, user);
        String country = (String) businessLogics.authenticationLM.country.read(session, user);
        return LocalePreferences.getLocale(language, country);
    }

    public static void saveClientLanguage(DataSession session, DataObject user, BusinessLogics businessLogics, String clientLanguage, String clientCountry, ExecutionStack stack) throws SQLException, SQLHandledException {
        if (clientLanguage != null) {
            businessLogics.authenticationLM.clientLanguage.change(clientLanguage, session, user);
            businessLogics.authenticationLM.clientCountry.change(clientCountry, session, user);
            session.applyException(businessLogics, stack);
        }
    }

    public static LogInfo readLogInfo(DataSession session, DataObject user, BusinessLogics businessLogics, String computerName, String remoteAddress) throws SQLException, SQLHandledException {
        String userName = (String) businessLogics.authenticationLM.logNameCustomUser.read(session, user);
        boolean allowExcessAllocatedBytes = businessLogics.serviceLM.allowExcessAllocatedBytes.read(session, user) != null;
        String userRoles = (String) businessLogics.securityLM.userRolesUser.read(session, user);
        return new LogInfo(allowExcessAllocatedBytes, userName, userRoles, computerName, remoteAddress);
    }

    public Locale getLocale() {
        return locale;
    }

    public Long getCurrentUser() {
        return user != null ? (Long)user.object : null;
    }

    protected String getCurrentAuthToken() {
        return authToken != null ? authToken.string : null;
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
                    while(true) { // we're doing greedy search for all subpathes to find appropriate "endpoint" action
                        int lastSlash = findActionName.lastIndexOf('/'); // if it is url
                        String checkActionName = findActionName;
                        if(lastSlash >= 0) // optimization
                            checkActionName = checkActionName.replace('/', '_');

                        if((action = businessLogics.findActionByCompoundName(checkActionName)) != null || lastSlash < 0)
                            break;

                        findActionName = findActionName.substring(0, lastSlash);
                    }
                    if (action != null) {
                        return executeExternal(action, actionName, false, request);
                    } else {
                        throw new RuntimeException(String.format("Action %s was not found", actionName));
                    }
                } else {
                    throw new RuntimeException("Action was not specified");
                }
            }, true, actionName, request);
    }

    @Override
    public ExternalResponse eval(boolean action, Object paramScript, ExternalRequest request) {
        String script = parseScript(paramScript, request.charsetName);

        return logFromExternalSystemRequest(() -> {
            if (script != null) {
                LA<?> runAction = businessLogics.evaluateRun(script, action);
                if(runAction != null) {
                    return executeExternal(runAction, paramScript, true, request);
                } else {
                    throw new RuntimeException("Action with name 'run' was not found");
                }
            } else {
                throw new RuntimeException("Eval script was not found");
            }
        }, false, script, request);
    }

    private String parseScript(Object paramScript, String charsetName) {
        try {
            return paramScript != null ? StringClass.text.parseHTTP(paramScript, Charset.forName(charsetName)) : null;
        } catch (ParseException e) {
            return null;
        }
    }

    private ExternalResponse executeExternal(LA<?> property, Object actionParam, boolean script, ExternalRequest request) {
        checkEnableApi(property, actionParam, script, request);

        RemoteNavigator.Notification runnable = new RemoteNavigator.Notification() {
            @Override
            public void run(ExecutionEnvironment env, ExecutionStack stack, PushAsyncResult asyncResult) {
                try {
                    RemoteConnection.this.executeExternal(property, request, env, stack);
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
            return new ResultExternalResponse(new Object[]{IntegerClass.instance.formatHTTP(RemoteNavigator.pushGlobalNotification(runnable), null)}, new String[0], new String[0], new String[0], new String[0], HttpServletResponse.SC_OK);
        else if(property.action.hasFlow(ChangeFlowType.INTERACTIVEWAIT)) {

            int mode = Settings.get().getExternalUINotificationMode();

            boolean serverNotification = mode >= 1;
            if(serverNotification) {
                boolean pendNotification = mode == 2;

                boolean foundNavigator = true;
                if (this instanceof RemoteNavigator)
                    ((RemoteNavigator) this).pushNotification(runnable);
                else
                    foundNavigator = logicsInstance.getNavigatorsManager().pushNotificationSession(sessionId, runnable, pendNotification);

                if (foundNavigator)
                    return new RedirectExternalResponse("/push-notification", null);
                else
                    return new RedirectExternalResponse("/", pendNotification ? null : RemoteNavigator.pushGlobalNotification(runnable));
            } else
                return new RedirectExternalResponse("/push-notification", RemoteNavigator.pushGlobalNotification(runnable));
        } else {
            try {
                ExecSession execSession = getExecSession();
                try {
                    DataSession dataSession = execSession.dataSession;

                    runnable.run(dataSession, getStack(), null);

                    return readResult(request.returnNames, property.action, dataSession);
                } finally {
                    execSession.close();
                }
            } catch (SQLException | SQLHandledException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private void executeExternal(LA<?> property, ExternalRequest request, ExecutionEnvironment env, ExecutionStack stack) throws SQLException, SQLHandledException, ParseException {
        writeRequestInfo(env, property.action, request);

        property.execute(env, stack, CallHTTPAction.getParams(env.getSession(), property, request.params, request.queryParams, Charset.forName(request.charsetName)));
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

        if(request.signature != null && logicsInstance.getSecurityManager().verifyData(ExternalUtils.generate(actionParam, script, request.params), request.signature))
            return;

        if(authException != null)
            throw authException;

        checkEnableApi(authToken.isAnonymous(), forceAPI);
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

    public void writeRequestInfo(ExecutionEnvironment env, Action<?> action, ExternalRequest request) throws SQLException, SQLHandledException {
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
        if (request.queryParams != null && action.uses(businessLogics.LM.params.property)) {
            MExclMap<ImList<Object>, String> mParams = MapFact.mExclMap();
            Map<String, Integer> paramIndexes = new HashMap<>();
            for (NameValuePair param : request.queryParams) {
                String paramName = param.getName();
                String paramValue = param.getValue();

                Integer paramIndex = paramIndexes.get(paramName);
                if(paramIndex == null)
                    paramIndex = 0;
                paramIndexes.put(paramName, paramIndex + 1);

                mParams.exclAdd(ListFact.toList(paramName, (Object) paramIndex), paramValue);
            }
            CallHTTPAction.writePropertyValues(session, env, businessLogics.LM.params, mParams.immutable());
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

    private ExternalResponse readResult(String[] returnNames, Action<?> property, DataSession dataSession) throws SQLException, SQLHandledException {
        List<Object> returns = new ArrayList<>();

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
                returns.add(formatReturnValue(returnProp.read(dataSession), returnProp.property));
        } else {
            Result<SessionDataProperty> resultProp = new Result<>();
            ObjectValue objectValue = businessLogics.LM.getExportValueProperty().readFirstNotNull(dataSession, resultProp, property);
            returns.add(formatReturnValue(objectValue.getValue(), resultProp.result));
        }

        ImOrderMap<String, String> headers = CallHTTPAction.readPropertyValues(dataSession, businessLogics.LM.headersTo).toOrderMap();
        String[] headerNames = headers.keyOrderSet().toArray(new String[headers.size()]);
        String[] headerValues = headers.valuesList().toArray(new String[headers.size()]);
        ImOrderMap<String, String> cookies = CallHTTPAction.readPropertyValues(dataSession, businessLogics.LM.cookiesTo).toOrderMap();
        String[] cookieNames = cookies.keyOrderSet().toArray(new String[cookies.size()]);
        String[] cookieValues = cookies.valuesList().toArray(new String[cookies.size()]);

        Integer statusHttp = (Integer) businessLogics.LM.statusHttpTo.read(dataSession);

        return new ResultExternalResponse(returns.toArray(), headerNames, headerValues, cookieNames, cookieValues, nvl(statusHttp, HttpServletResponse.SC_OK));
    }

    private Object formatReturnValue(Object returnValue, Property returnProperty) {
        return returnProperty.getType().formatHTTP(returnValue, null);
    }

    protected abstract ExecSession getExecSession() throws SQLException;

    public class ExecSession {

        public DataSession dataSession;

        public ExecSession(DataSession dataSession) {
           this.dataSession = dataSession;
        }

        public void close() throws SQLException {
        }
    }

    private ExternalResponse logFromExternalSystemRequest(Callable<ExternalResponse> responseCallable, boolean exec, String action, ExternalRequest request) {
        String requestLogMessage = Settings.get().isLogFromExternalSystemRequests() ? getExternalSystemRequestsLog(logInfo, request.servletPath, request.method,
                "\tREQUEST_QUERY: " + request.query + "\n" + "\t" + (exec ? "ACTION" : "SCRIPT") + ":\n\t\t " + action) : null;
        boolean successfulResponse = false;
        try {
            ExternalResponse execResult = responseCallable.call();

            successfulResponse = successfulResponse(execResult.getStatusHttp());

            if (requestLogMessage != null && Settings.get().isLogFromExternalSystemRequestsDetail()) {
                List<NameValuePair> queryParams = request.queryParams;
                ExternalUtils.ExternalResponse externalResponse = ExternalUtils.getExternalResponse(execResult, queryParams != null ? queryParams : Collections.emptyList(), null);

                if(externalResponse instanceof ExternalUtils.ResultExternalResponse) {
                    ExternalUtils.ResultExternalResponse result = (ExternalUtils.ResultExternalResponse) externalResponse;
                    requestLogMessage += getExternalSystemRequestsLogDetail(BaseUtils.toStringMap(request.headerNames, request.headerValues),
                            BaseUtils.toStringMap(request.cookieNames, request.cookieValues),
                            request.body != null ? new String(request.body, Charset.forName(request.charsetName)) : null,
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
