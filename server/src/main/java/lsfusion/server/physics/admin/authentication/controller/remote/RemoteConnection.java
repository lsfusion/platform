package lsfusion.server.physics.admin.authentication.controller.remote;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.server.base.controller.remote.RemoteRequestObject;
import lsfusion.server.base.controller.thread.SyncType;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.navigator.controller.env.*;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.log.LogInfo;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public abstract class RemoteConnection extends RemoteRequestObject {

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
        return dbManager.createSession(sql, new WeakUserController(this), createFormController(), new WeakTimeoutController(this), createChangesController(), new WeakLocaleController(this), null);
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

            initUserContext(hostName, connectionInfo.hostAddress, connectionInfo.language, connectionInfo.country, stack, session);
        }
    }

    protected void initUser(SecurityManager securityManager, AuthenticationToken token, DataSession session) throws SQLException, SQLHandledException {
        String login = securityManager.parseToken(token);
        authToken = token;
        user = login != null ? securityManager.readUser(login, session) : (SystemProperties.inDevMode ? securityManager.getAdminUser() : securityManager.getAnonymousUser());
    }

    // in theory its possible to cache all this
    // locale + log info
    protected void initUserContext(String hostName, String remoteAddress, String clientLanguage, String clientCountry, ExecutionStack stack, DataSession session) throws SQLException, SQLHandledException {
        logInfo = readLogInfo(session, user, businessLogics, hostName, remoteAddress);
        locale = readLocale(session, user, businessLogics, clientLanguage, clientCountry, stack);
        userRole = (Long) businessLogics.securityLM.firstRoleUser.read(session, user);
        transactionTimeout = (Integer) businessLogics.serviceLM.transactTimeoutUser.read(session, user);
    }

    public boolean changeCurrentUser(DataObject user, ExecutionStack stack) throws SQLException, SQLHandledException {
        this.user = user;
        try(DataSession session = createSession()) {
            initUserContext(logInfo.hostnameComputer, logInfo.remoteAddress, null, null, stack, session);
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
        String userName = (String) businessLogics.authenticationLM.nameContact.read(session, user);
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
    protected ServerResponse prepareResponse(long requestIndex, List<ClientAction> pendingActions, ExecutionStack stack) {
        return null;
    }
}
