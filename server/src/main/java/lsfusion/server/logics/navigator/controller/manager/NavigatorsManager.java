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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

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
    private static class PendingNotification {
        private final Function<RemoteNavigator, Long> check;
        private final Consumer<RemoteNavigator> run;
        private final long started;

        public PendingNotification(Function<RemoteNavigator, Long> check, Consumer<RemoteNavigator> run) {
            this.check = check;
            this.run = run;
            this.started = System.currentTimeMillis();
        }
    }
    private final List<PendingNotification> pendingNotifications = new ArrayList<>();

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
            RemoteNavigator navigator = new RemoteNavigator(rmiManager.getPort(), logicsInstance, token, navigatorInfo, stack);

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
            businessLogics.systemEventsLM.computerConnection.change(navigator.getComputer(), session, newConnection);
            businessLogics.systemEventsLM.connectionStatusConnection.change(businessLogics.systemEventsLM.connectionStatus.getObjectID("connectedConnection"), session, newConnection);
            businessLogics.systemEventsLM.connectTimeConnection.change(businessLogics.timeLM.currentDateTime.readClasses(session), session, newConnection);

            businessLogics.systemEventsLM.schemeConnection.change(navigatorInfo.session.externalRequest.scheme, session, newConnection);
            businessLogics.systemEventsLM.remoteAddressConnection.change(navigator.getLogInfo().remoteAddress, session, newConnection);
            businessLogics.systemEventsLM.webHostConnection.change(navigatorInfo.session.externalRequest.webHost, session, newConnection);
            businessLogics.systemEventsLM.webPortConnection.change(navigatorInfo.session.externalRequest.webPort, session, newConnection);
            businessLogics.systemEventsLM.contextPathConnection.change(navigatorInfo.session.externalRequest.contextPath, session, newConnection);
            businessLogics.systemEventsLM.servletPathConnection.change(navigatorInfo.session.externalRequest.servletPath, session, newConnection);
            businessLogics.systemEventsLM.pathInfoConnection.change(navigatorInfo.session.externalRequest.pathInfo, session, newConnection);
            businessLogics.systemEventsLM.queryConnection.change(navigatorInfo.session.externalRequest.query, session, newConnection);

            businessLogics.systemEventsLM.launchConnection.change(businessLogics.systemEventsLM.currentLaunch.readClasses(session), session, newConnection);
            String result = session.applyMessage(businessLogics, stack);
            if(result != null)
                throw new RemoteMessageException(result);
        }

        synchronized (navigators) {
            if (newConnection != null) {
                navigator.setConnection(new DataObject((Long) newConnection.object, businessLogics.systemEventsLM.connection)); // to update classes after apply
            }
            navigators.add(navigator);
        }
    }

    public void navigatorInitialized(RemoteNavigator navigator) {
        List<Consumer<RemoteNavigator>> runNotifications = new ArrayList<>();
        synchronized (navigators) {
            for (Iterator<PendingNotification> iterator = pendingNotifications.iterator(); iterator.hasNext(); ) {
                PendingNotification pendingNotification = iterator.next();
                if (pendingNotification.check.apply(navigator) > 0) {
                    runNotifications.add(pendingNotification.run);
                    iterator.remove();
                }
                if (System.currentTimeMillis() - pendingNotification.started > 600000)
                    iterator.remove();
            }
        }
        for (Consumer<RemoteNavigator> runNotification : runNotifications)
            runNotification.accept(navigator);
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

    public void pushNotificationConnection(DataObject connectionObject, RemoteNavigator.Notification run) {
        pushNotificationConnection(connectionObject, navigator -> navigator.pushNotification(run));
    }

    public boolean pushNotificationSession(String sessionId, RemoteNavigator.Notification run, boolean pend) {
        return pushNotification(navigator -> navigator.active && navigator.sessionId != null && navigator.sessionId.equals(sessionId) ? navigator.getContext().getUserLastActivity() : 0L, navigator -> navigator.pushNotification(run), pend);
    }

    public void shutdownConnection(DataObject connectionObject) {
        pushNotificationConnection(connectionObject, navigator -> {
            try {
                navigator.close();
            } catch (RemoteException e) {
                logger.error(ThreadLocalContext.localize("{logics.server.remote.exception.on.shutdown.client}"), e);
            }
        });
    }

    public void pushNotificationConnection(DataObject connectionObject, Consumer<RemoteNavigator> run) {
        pushNotification(navigator -> navigator.getConnection() != null && navigator.getConnection().equals(connectionObject) ? 1L : 0L, run, false);
    }

    public boolean pushNotification(Function<RemoteNavigator, Long> check, Consumer<RemoteNavigator> run, boolean pend) {
        RemoteNavigator foundNavigator = null;
        synchronized (navigators) {
            long bestPriority = 0;
            for (RemoteNavigator navigator : navigators) {
                if (navigator != null) {
                    long priority = check.apply(navigator);
                    if (priority > bestPriority) {
                        foundNavigator = navigator;
                        bestPriority = priority;
                    }
                }
            }
            if(foundNavigator == null && pend)
                pendingNotifications.add(new PendingNotification(check, run));
        }
        if(foundNavigator != null) {
            run.accept(foundNavigator);
            return true;
        }
        return false;
    }
}
