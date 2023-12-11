package lsfusion.server.physics.admin.authentication.controller.remote;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.connection.RemoteConnectionInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.ExternalUtils;
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
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.navigator.controller.env.*;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.log.LogInfo;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.external.to.ExternalHTTPAction;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;

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

    public RemoteConnection(int port, String sID, ExecutionStack upStack) throws RemoteException {
        super(port, upStack, sID, SyncType.NOSYNC);
    }
    
    protected abstract FormController createFormController();
    protected abstract ChangesController createChangesController();    

    protected DataSession createSession() throws SQLException {
        return dbManager.createSession(sql, new WeakUserController(this), createFormController(), new WeakTimeoutController(this), createChangesController(), new WeakLocaleController(this), dbManager.getIsServerRestartingController(), null);
    }

    protected void initContext(LogicsInstance logicsInstance, AuthenticationToken token, ConnectionInfo connectionInfo, ExecutionStack stack) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLHandledException {
        this.businessLogics = logicsInstance.getBusinessLogics();
        this.dbManager = logicsInstance.getDbManager();
        this.sql = dbManager.createSQL(new WeakSQLSessionContextProvider(this));
        
        this.logicsInstance = logicsInstance;

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
            return logRequest(() -> {
                ExternalResponse result;
                try {
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
                            result = executeExternal(action, request);
                        } else {
                            throw new RuntimeException("Action %s was not found");
                        }
                    } else {
                        throw new RuntimeException("Action was not specified");
                    }
                } catch (ParseException | SQLHandledException | SQLException | IOException e) {
                    throw new RuntimeException(e);
                }
                return result;
            }, true, actionName, request);
    }

    @Override
    public ExternalResponse eval(boolean action, Object paramScript, ExternalRequest request) {
        String script = parseScript(paramScript, request.charsetName);

        return logRequest(() -> {
            ExternalResponse result;
            if (script != null) {
                try {
                    LA<?> runAction = businessLogics.evaluateRun(script, action);
                    if(runAction != null) {
                        result = executeExternal(runAction, request);
                    } else {
                        throw new RuntimeException("Action with name 'run' was not found");
                    }
                } catch (SQLException | ParseException | SQLHandledException | IOException e) {
                    throw Throwables.propagate(e);
                }
            } else {
                throw new RuntimeException("Eval script was not found");
            }
            return result;
        }, false, script, request);
    }

    private String parseScript(Object paramScript, String charsetName) {
        try {
            return paramScript != null ? StringClass.text.parseHTTP(paramScript, Charset.forName(charsetName)) : null;
        } catch (ParseException e) {
            return null;
        }
    }

    private ExternalResponse executeExternal(LA<?> property, ExternalRequest request) throws SQLException, ParseException, SQLHandledException, IOException {
        ExecSession execSession = getExecSession();
        try {
            DataSession dataSession = execSession.dataSession;

            checkEnableApi(property);

            writeRequestInfo(dataSession, property.action, request);

            property.execute(dataSession, getStack(), ExternalHTTPAction.getParams(dataSession, property, request.params, Charset.forName(request.charsetName)));

            return readResult(request.returnNames, property.action, dataSession);
        } finally {
            execSession.close();
        }
    }

    protected AuthenticationException authException;

    private void checkEnableApi(LA<?> property) {
        boolean forceAPI = false;
        String annotation = property.action.annotation;
        if(annotation != null) {
            if(annotation.equals("noauth"))
                return;
            forceAPI = annotation.equals("api");
        }
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

    public void writeRequestInfo(DataSession session, Action<?> action, ExternalRequest request) throws SQLException, SQLHandledException {
        ExecutionEnvironment env = session;
        if (action.uses(businessLogics.LM.headers.property)) {
            ExternalHTTPAction.writePropertyValues(session, env, businessLogics.LM.headers, request.headerNames, request.headerValues);
        }
        if (action.uses(businessLogics.LM.cookies.property)) {
            String[] cookieNames = request.cookieNames;
            String[] cookieValues = request.cookieValues;
            ExternalHTTPAction.writePropertyValues(session, env, businessLogics.LM.cookies, cookieNames == null ? new String[0] : cookieNames, cookieValues == null ? new String[0] : cookieValues);
        }
        if (action.uses(businessLogics.LM.query.property)) {
            businessLogics.LM.query.change(request.query, session);
        }
        if (request.query != null && action.uses(businessLogics.LM.params.property)) {
            List<String> paramNames = new ArrayList<>();
            List<String> paramValues = new ArrayList<>();
            for (String param : request.query.split("[&?]")) {
                String[] splittedParam = param.split("=");
                if (splittedParam.length == 2) {
                    paramNames.add(splittedParam[0]);
                    paramValues.add(splittedParam[1]);
                }
            }
            ExternalHTTPAction.writePropertyValues(session, env, businessLogics.LM.params, paramNames.toArray(new String[0]), paramValues.toArray(new String[0]));
        }
        if (action.uses(businessLogics.LM.contentType.property)) {
            businessLogics.LM.contentType.change(request.contentType, session);
        }
        if (action.uses(businessLogics.LM.body.property)) {
            businessLogics.LM.body.change(new RawFileData(request.body), session);
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

        ImOrderMap<String, String> headers = ExternalHTTPAction.readPropertyValues(dataSession, businessLogics.LM.headersTo).toOrderMap();
        String[] headerNames = headers.keyOrderSet().toArray(new String[headers.size()]);
        String[] headerValues = headers.valuesList().toArray(new String[headers.size()]);
        ImOrderMap<String, String> cookies = ExternalHTTPAction.readPropertyValues(dataSession, businessLogics.LM.cookiesTo).toOrderMap();
        String[] cookieNames = cookies.keyOrderSet().toArray(new String[cookies.size()]);
        String[] cookieValues = cookies.valuesList().toArray(new String[cookies.size()]);

        Integer statusHttp = (Integer) businessLogics.LM.statusHttpTo.read(dataSession);

        return new ExternalResponse(returns.toArray(), headerNames, headerValues, cookieNames, cookieValues, statusHttp);
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

    public static String getRequestLog(ExternalRequest request, LogInfo logInfo, boolean exec, String action) {
        return "\nREQUEST:\n" +
                "\tREQUEST_USER_INFO: " + logInfo.toString() + "\n" +
                "\tREQUEST_PATH: " + request.servletPath + "\n" +
                "\tREQUEST_QUERY: " + request.query + "\n" +
                "\tREQUEST_METHOD: " + request.method + "\n" +
                "\t" + (exec ? "ACTION" : "SCRIPT") + ":\n\t\t " + action + "\n";
    }

    public static String getRequestLogDetail(ExternalRequest request, ExternalResponse response) {
        Charset charset = ExternalUtils.getCharsetFromContentType(request.contentType == null ? null : ContentType.parse(request.contentType));
        List<NameValuePair> queryParams = URLEncodedUtils.parse(request.query, charset);
        String returnMultiType = ExternalUtils.getParameterValue(queryParams, ExternalUtils.RETURNMULTITYPE_PARAM);
        boolean returnBodyUrl = returnMultiType != null && returnMultiType.equals("bodyurl");
        HttpEntity httpEntity = ExternalUtils.getInputStreamFromList(response.results, ExternalUtils.getBodyUrl(response.results, returnBodyUrl), null, new ArrayList<>(), null, null);

        return "\n" + getHeadersString(request.headerNames, request.headerValues) +
                getCookiesString(request.cookieNames, request.cookieValues) +
                "\tBODY:\n\t\t" +
                (request.body != null ? new String(request.body, Charset.forName(request.charsetName)) : "") +
                "\n\nRESPONSE:\n" +
                "\tOBJECTS:\n\t\t" + httpEntity + "\n" +
                getHeadersString(response.headerNames, response.headerValues) +
                getCookiesString(response.cookieNames, response.cookieValues) +
                "\n\tRESPONSE_STATUS_HTTP: " + BaseUtils.nvl(response.statusHttp, HttpServletResponse.SC_OK) + "\n";
    }

    private static String getHeadersString(String[] headerNames, String[] headerValues) {
        StringBuilder headers = new StringBuilder().append("\tHEADERS:\n");
        if (headerNames != null) {
            for (int i = 0; i < headerNames.length; i++) {
                headers.append("\t\t").append(headerNames[i]).append(": ").append(headerValues[i]).append("\n");
            }
        }
        return headers.toString();
    }

    private static String getCookiesString(String[] cookieNames, String[] cookieValues) {
        StringBuilder cookies = new StringBuilder().append("\tCOOKIES:\n");
        if (cookieNames != null) {
            for (int i = 0; i < cookieNames.length; i++) {
                cookies.append("\t\t").append(cookieNames[i]).append(": ").append(cookieValues[i]).append("\n");
            }
        }
        return cookies.toString();
    }

    private ExternalResponse logRequest(Callable<ExternalResponse> responseCallable, boolean exec, String action, ExternalRequest request) {
        String requestLogMessage = Settings.get().isLogExternalRequests() ? getRequestLog(request, logInfo, exec, action) : null;
        try {
            ExternalResponse response = responseCallable.call();
            if (requestLogMessage != null) {

                if (Settings.get().isLogExternalRequestsDetail())
                    requestLogMessage += getRequestLogDetail(request, response);

                ServerLoggers.httpServerLogger.info(requestLogMessage);
            }

            return response;
        } catch (Throwable t) {
            if (requestLogMessage != null) {
                requestLogMessage += "\n\tERROR: " + t.getMessage() + "\n";
                ServerLoggers.httpServerLogger.error(requestLogMessage);
            }

            throw Throwables.propagate(t);
        }
    }
}
