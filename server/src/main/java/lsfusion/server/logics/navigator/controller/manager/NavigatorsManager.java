package lsfusion.server.logics.navigator.controller.manager;

import com.google.common.base.Throwables;
import lsfusion.base.ApiResourceBundle;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.heavy.weak.WeakIdentityHashSet;
import lsfusion.interop.base.exception.RemoteMessageException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.navigator.NavigatorInfo;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import lsfusion.server.base.controller.lifecycle.LifecycleEvent;
import lsfusion.server.base.controller.manager.LogicsManager;
import lsfusion.server.base.controller.remote.RmiManager;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.controller.stack.EnvStackRunnable;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.controller.manager.RestartManager;
import lsfusion.server.logics.navigator.controller.remote.RemoteNavigator;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NavigatorsManager extends LogicsManager implements InitializingBean {
    private static final Logger logger = Logger.getLogger(NavigatorsManager.class);

    //время жизни неиспользуемого навигатора - 3 часа по умолчанию
    public static final long MAX_FREE_NAVIGATOR_LIFE_TIME = Long.parseLong(System.getProperty("lsfusion.server.navigatorMaxLifeTime", Long.toString(3L * 3600L * 1000L)));

    private LogicsInstance logicsInstance;

    private BusinessLogics businessLogics;

    @Override
    protected BusinessLogics getBusinessLogics() {
        return businessLogics;
    }

    private BaseLogicsModule baseLM;

    private RestartManager restartManager;

    private SecurityManager securityManager;

    private RmiManager rmiManager;

    private DBManager dbManager;

//    private ScheduledExecutorService executor;

    // synchronize'ся везде
    private final WeakIdentityHashSet<RemoteNavigator> navigators = new WeakIdentityHashSet<>();

    private AtomicBoolean removeExpiredScheduled = new AtomicBoolean(false);

    public NavigatorsManager() {
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    public void setBusinessLogics(BusinessLogics businessLogics) {
        this.businessLogics = businessLogics;
    }

    public void setRestartManager(RestartManager restartManager) {
        this.restartManager = restartManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setRmiManager(RmiManager rmiManager) {
        this.rmiManager = rmiManager;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(logicsInstance, "logicsInstance must be specified");
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(restartManager, "restartManager must be specified");
        Assert.notNull(securityManager, "securityManager must be specified");
        Assert.notNull(rmiManager, "rmiManager must be specified");
        Assert.notNull(dbManager, "dbManager must be specified");
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        baseLM = businessLogics.LM;
//        executor = Executors.newSingleThreadScheduledExecutor(new ContextAwareDaemonThreadFactory(logicsInstance.getContext(), "navigator-manager-daemon"));
    }
    
    private DataSession createSession() throws SQLException {
        return dbManager.createSession();
    }

    public RemoteNavigatorInterface createNavigator(ExecutionStack stack, AuthenticationToken token, NavigatorInfo navigatorInfo) {
        try {
            RemoteNavigator navigator =  new RemoteNavigator(rmiManager.getPort(), logicsInstance, token, navigatorInfo, stack);

            if (restartManager.isPendingRestart() && !BaseUtils.hashEquals(navigator.getUser(), securityManager.getAdminUser()))
                throw new RemoteMessageException(ApiResourceBundle.getString("exceptions.server.is.restarting"));

            return navigator;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void navigatorCreated(ExecutionStack stack, RemoteNavigator navigator, NavigatorInfo navigatorInfo) throws SQLException, SQLHandledException {
        DataObject newConnection;

        try (DataSession session = createSession()) {
            newConnection = session.addObject(businessLogics.systemEventsLM.connection);
            businessLogics.systemEventsLM.userConnection.change(navigator.getUser(), session, newConnection);
            businessLogics.systemEventsLM.osVersionConnection.change(navigatorInfo.osVersion, session, newConnection);
            businessLogics.systemEventsLM.processorConnection.change(navigatorInfo.processor, session, newConnection);
            businessLogics.systemEventsLM.architectureConnection.change(navigatorInfo.architecture, session, newConnection);
            businessLogics.systemEventsLM.coresConnection.change(navigatorInfo.cores, session, newConnection);
            businessLogics.systemEventsLM.physicalMemoryConnection.change(navigatorInfo.physicalMemory, session, newConnection);
            businessLogics.systemEventsLM.totalMemoryConnection.change(navigatorInfo.totalMemory, session, newConnection);
            businessLogics.systemEventsLM.maximumMemoryConnection.change(navigatorInfo.maximumMemory, session, newConnection);
            businessLogics.systemEventsLM.freeMemoryConnection.change(navigatorInfo.freeMemory, session, newConnection);
            businessLogics.systemEventsLM.javaVersionConnection.change(navigatorInfo.javaVersion, session, newConnection);
            businessLogics.systemEventsLM.is64JavaConnection.change(navigatorInfo.javaVersion != null && navigatorInfo.javaVersion.endsWith("64 bit"), session, newConnection);
            businessLogics.systemEventsLM.screenSizeConnection.change(navigatorInfo.screenSize, session, newConnection);
            businessLogics.systemEventsLM.computerConnection.change(navigator.getComputer(), session, newConnection);
            businessLogics.systemEventsLM.clientTypeConnection.change(businessLogics.systemEventsLM.clientType.getObjectID(navigatorInfo.clientType.toString()), session, newConnection);
            businessLogics.systemEventsLM.connectionStatusConnection.change(businessLogics.systemEventsLM.connectionStatus.getObjectID("connectedConnection"), session, newConnection);
            businessLogics.systemEventsLM.connectTimeConnection.change(businessLogics.timeLM.currentDateTime.readClasses(session), session, newConnection);
            businessLogics.systemEventsLM.remoteAddressConnection.change(navigator.getLogInfo().remoteAddress, session, newConnection);
            businessLogics.systemEventsLM.launchConnection.change(businessLogics.systemEventsLM.currentLaunch.readClasses(session), session, newConnection);
            String result = session.applyMessage(businessLogics, stack);
            if(result != null)
                throw new RemoteMessageException(result);
        }

        synchronized (navigators) {
            if (newConnection != null) {
                navigator.setConnection(new DataObject(newConnection.object, businessLogics.systemEventsLM.connection)); // to update classes after apply
            }
            navigators.add(navigator);
        }
    }

    public void navigatorClosed(RemoteNavigator navigator, ExecutionStack stack, DataObject connection) {
        synchronized (navigators) {
            navigators.remove(navigator);
            if (navigators.isEmpty()) {
                restartManager.forcedRestartIfPending();
            }
        }
        try {
            try (DataSession session = createSession()) {
                if (connection != null) {
                    businessLogics.systemEventsLM.connectionStatusConnection.change(businessLogics.systemEventsLM.connectionStatus.getObjectID("disconnectedConnection"), session, connection);
                } else
                    ServerLoggers.assertLog(false, "SHOULD NOT BE");
                apply(session, stack);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void pushNotificationCustomUser(DataObject connectionObject, EnvStackRunnable run) {
        RemoteNavigator navigator = findNavigator(connectionObject);
        if (navigator != null) {
            try {
                navigator.pushNotification(run);
            } catch (RemoteException e) {
                logger.error(ThreadLocalContext.localize("{logics.server.remote.exception.on.push.action}"), e);
            }
        }
    }

    public void shutdownConnection(DataObject connectionObject) {
        RemoteNavigator navigator = findNavigator(connectionObject);
        if (navigator != null) {
            try {
                navigator.close();
            } catch (RemoteException e) {
                logger.error(ThreadLocalContext.localize("{logics.server.remote.exception.on.shutdown.client}"), e);
            }
        }
    }

    private RemoteNavigator findNavigator(DataObject connectionObject) {
        synchronized (navigators) {
            for (RemoteNavigator navigator : navigators) {
                if (navigator != null) {
                    if (navigator.getConnection() != null && navigator.getConnection().equals(connectionObject)) {
                        return navigator;
                    }
                }
            }
            return null;
        }
    }
}
