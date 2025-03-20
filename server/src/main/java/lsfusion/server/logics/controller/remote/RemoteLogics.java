package lsfusion.server.logics.controller.remote;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.io.Resources;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.lambda.CallableWithParam;
import lsfusion.interop.base.exception.RemoteMessageException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.authentication.Authentication;
import lsfusion.interop.logics.remote.RemoteClientInterface;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.navigator.NavigatorInfo;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.SessionInfo;
import lsfusion.interop.session.remote.RemoteSessionInterface;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.lifecycle.LifecycleListener;
import lsfusion.server.base.controller.remote.RmiManager;
import lsfusion.server.base.controller.remote.context.ContextAwarePendingRemoteObject;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.session.controller.remote.RemoteSession;
import lsfusion.server.logics.controller.manager.RestartManager;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.navigator.controller.manager.NavigatorsManager;
import lsfusion.server.logics.navigator.controller.remote.RemoteNavigator;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.log.RemoteLoggerAspect;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class RemoteLogics<T extends BusinessLogics> extends ContextAwarePendingRemoteObject implements RemoteLogicsInterface, InitializingBean, LifecycleListener {
    protected final static Logger logger = ServerLoggers.remoteLogger;

    protected T businessLogics;
    protected BaseLogicsModule baseLM;

    protected NavigatorsManager navigatorsManager;

    private RmiManager rmiManager; // for sessions

    protected RestartManager restartManager;
    
    protected SecurityManager securityManager;

    protected DBManager dbManager;

    public void setBusinessLogics(T businessLogics) {
        this.businessLogics = businessLogics;
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        setContext(logicsInstance.getContext());
    }

    public void setNavigatorsManager(NavigatorsManager navigatorsManager) {
        this.navigatorsManager = navigatorsManager;
    }

    public void setRmiManager(RmiManager rmiManager) {
        this.rmiManager = rmiManager;
    }

    public void setRestartManager(RestartManager restartManager) {
        this.restartManager = restartManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    public RemoteLogics() {
        super("logics");
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(navigatorsManager, "navigatorsManager must be specified");
        Assert.notNull(restartManager, "restartManager must be specified");
        Assert.notNull(securityManager, "securityManager must be specified");
        Assert.notNull(dbManager, "dbManager must be specified");
        //assert logicsInstance by checking the context
        Assert.notNull(getContext(), "logicsInstance must be specified");
    }

    @Override
    public int getOrder() {
        return BLLOADER_ORDER - 1;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (LifecycleEvent.INIT.equals(event.getType())) {
            this.baseLM = businessLogics.LM;
        }
    }

    public RemoteNavigatorInterface createNavigator(AuthenticationToken token, NavigatorInfo navigatorInfo) {
        RemoteNavigator.checkEnableUI(token);
        checkClientVersion(navigatorInfo);
        
        return navigatorsManager.createNavigator(getStack(), token, navigatorInfo);
    }

    private void checkClientVersion(NavigatorInfo navigatorInfo) {
        String error = BaseUtils.checkClientVersion(BaseUtils.getPlatformVersion(), BaseUtils.getApiVersion(),
                                                 navigatorInfo.platformVersion, navigatorInfo.apiVersion);
        if(error != null) {
            throw new RemoteMessageException(error);
        }
    }

    @Override
    public RemoteSessionInterface createSession(AuthenticationToken token, SessionInfo sessionInfo) throws RemoteException {
        try {
            return createSession(rmiManager.getPort(), token, sessionInfo);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public RemoteSession createSession(int port, AuthenticationToken token, SessionInfo sessionInfo) throws RemoteException, SQLException, SQLHandledException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new RemoteSession(port, getContext().getLogicsInstance(), token, sessionInfo, getStack());
    }

    private final Set<RemoteSession> sessionsPool = ConcurrentHashMap.newKeySet();
    private RemoteSession popSession(Predicate<RemoteSession> check) {
        for(RemoteSession poolSession : sessionsPool) {
            if(check.apply(poolSession)) {
                if(sessionsPool.remove(poolSession)) // remove is consistent and atomic
                    return poolSession;
            }
        }
        return null;
    }
    
    private ExternalResponse runInNewSession(AuthenticationToken token, ConnectionInfo connectionInfo, ExternalRequest request, CallableWithParam<RemoteSession, ExternalResponse> callable) throws RemoteException {
        RemoteSession session = null;
        boolean closeSession = false;
        try {
            if(!Settings.get().isReinitAPISession())
                session = popSession(poolSession -> poolSession.equalsConnectionContext(token, connectionInfo));
            if(session == null) {
                session = popSession(poolSession -> true);
                if(session != null)
                    session.initConnectionContext(token, connectionInfo, getStack());
            }
            if(session == null)
                session = createSession(-1, token, new SessionInfo(connectionInfo, request));

            return callable.call(session);
        } catch (Throwable t) {
            closeSession = true;
            throw Throwables.propagate(t);
        } finally {
            if(session != null) {
                RemoteSession fSession = session;
                boolean fCloseSession = closeSession;
                scheduleClose(0, () -> {
                    if (fCloseSession || sessionsPool.size() >= Settings.get().getFreeAPISessions())
                        fSession.localClose();
                    else {
                        fSession.clean();
                        sessionsPool.add(fSession);
                    }
                });
            }
        }
    }

    @Override
    public ExternalResponse exec(AuthenticationToken token, ConnectionInfo connectionInfo, final String action, final ExternalRequest request) throws RemoteException {
        return runInNewSession(token, connectionInfo, request, session -> session.exec(action, request));
    }

    @Override
    public ExternalResponse eval(AuthenticationToken token, ConnectionInfo connectionInfo, final boolean action, final ExternalRequest.Param paramScript, final ExternalRequest request) throws RemoteException {
        return runInNewSession(token, connectionInfo, request, session -> session.eval(action, paramScript, request));
    }

    public void ping() throws RemoteException {
        //for filterIncl-alive
    }

    public void sendPingInfo(String computerName, Map<Long, List<Long>> pingInfoMap) {
        Map<Long, List<Long>> pingInfoEntry = RemoteLoggerAspect.pingInfoMap.get(computerName);
        pingInfoEntry = pingInfoEntry != null ? pingInfoEntry : MapFact.getGlobalConcurrentHashMap();
        pingInfoEntry.putAll(pingInfoMap);
        RemoteLoggerAspect.pingInfoMap.put(computerName, pingInfoEntry);
    }

    @Override
    public AuthenticationToken authenticateUser(Authentication authentication) throws RemoteException {
        return securityManager.authenticateUser(authentication, getStack());
    }

    @Override
    public long generateID() throws RemoteException {
        return dbManager.generateID();
    }

    @Override
    protected boolean isEnabledUnreferenced() { // иначе когда отключаются все клиенты логика закрывается
        return false;
    }

    @Override
    protected Set<Thread> getAllContextThreads() {
        return null;
    }

    @Override
    public List<String> saveAndGetCustomReportPathList(String formSID, boolean recreate) throws RemoteException {
        try {
            return FormInstance.saveAndGetCustomReportPathList(businessLogics.findForm(formSID), recreate);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Object getProfiledObject() {
        return "l";
    }

    public byte[] findClass(String name) throws RemoteException {
        try {
            return Resources.toByteArray(Resources.getResource(name.replace('.', '/') + ".class"));
        } catch (IOException e) {
            throw new RuntimeException(localize("{logics.error.reading.class.on.the.server}"), e);
        }
    }

    @Override
    public void registerClient(RemoteClientInterface client) throws RemoteException {
        rmiManager.registerClient(client);
    }
}

