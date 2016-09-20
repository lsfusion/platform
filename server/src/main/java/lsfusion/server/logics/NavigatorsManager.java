package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.NavigatorInfo;
import lsfusion.base.WeakIdentityHashSet;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.server.EnvStackRunnable;
import lsfusion.server.ServerLoggers;
import lsfusion.server.auth.User;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.navigator.RemoteNavigator;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.lifecycle.LogicsManager;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.session.DataSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class NavigatorsManager extends LogicsManager implements InitializingBean {
    private static final Logger logger = Logger.getLogger(NavigatorsManager.class);

    //время жизни неиспользуемого навигатора - 3 часа по умолчанию
    public static final long MAX_FREE_NAVIGATOR_LIFE_TIME = Long.parseLong(System.getProperty("lsfusion.server.navigatorMaxLifeTime", Long.toString(3L * 3600L * 1000L)));

    private LogicsInstance logicsInstance;

    private BusinessLogics<?> businessLogics;

    private BaseLogicsModule<?> baseLM;

    private RestartManager restartManager;

    private SecurityManager securityManager;

    private RMIManager rmiManager;

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

    public void setBusinessLogics(BusinessLogics<?> businessLogics) {
        this.businessLogics = businessLogics;
    }

    public void setRestartManager(RestartManager restartManager) {
        this.restartManager = restartManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setRmiManager(RMIManager rmiManager) {
        this.rmiManager = rmiManager;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
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

    public RemoteNavigatorInterface createNavigator(ExecutionStack stack, boolean isFullClient, NavigatorInfo navigatorInfo, boolean reuseSession) {
        //пока отключаем механизм восстановления сессии... т.к. он не работает с текущей схемой последовательных запросов в форме
        reuseSession = false;

        //логика EXPIRED навигаторов неактуальна, пока не работает механизм восстановления сессии
//        scheduleRemoveExpired();

        try {
            User user;
            try (DataSession session = dbManager.createSession()) {
                user = securityManager.authenticateUser(session, navigatorInfo.login, navigatorInfo.password, stack);
                session.apply(businessLogics, stack);
            }

//            if (reuseSession) {
//                List<RemoteNavigator> navigatorsList = navigators.get(loginKey);
//                if (navigatorsList != null) {
//                    for(RemoteNavigator navigator : navigatorsList) {
//                        navigator.disconnect();
//                        navigator.unexportAndClean();
//                        removeNavigator(stack, loginKey);
//                    }
//                }
//            }

            return new RemoteNavigator(logicsInstance, isFullClient, navigatorInfo, user, rmiManager.getExportPort(), stack);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void navigatorCreated(ExecutionStack stack, RemoteNavigator navigator, NavigatorInfo navigatorInfo) throws SQLException, SQLHandledException {
        DataObject newConnection = null;

        if(!securityManager.isUniversalPassword(navigatorInfo.password)) {
            try (DataSession session = dbManager.createSession()) {
                newConnection = session.addObject(businessLogics.systemEventsLM.connection);
                businessLogics.systemEventsLM.userConnection.change(navigator.getUser().object, session, newConnection);
                businessLogics.systemEventsLM.osVersionConnection.change(navigatorInfo.osVersion, session, newConnection);
                businessLogics.systemEventsLM.processorConnection.change(navigatorInfo.processor, session, newConnection);
                businessLogics.systemEventsLM.architectureConnection.change(navigatorInfo.architecture, session, newConnection);
                businessLogics.systemEventsLM.coresConnection.change(navigatorInfo.cores, session, newConnection);
                businessLogics.systemEventsLM.physicalMemoryConnection.change(navigatorInfo.physicalMemory, session, newConnection);
                businessLogics.systemEventsLM.totalMemoryConnection.change(navigatorInfo.totalMemory, session, newConnection);
                businessLogics.systemEventsLM.maximumMemoryConnection.change(navigatorInfo.maximumMemory, session, newConnection);
                businessLogics.systemEventsLM.freeMemoryConnection.change(navigatorInfo.freeMemory, session, newConnection);
                businessLogics.systemEventsLM.javaVersionConnection.change(navigatorInfo.javaVersion, session, newConnection);
                businessLogics.systemEventsLM.screenSizeConnection.change(navigatorInfo.screenSize, session, newConnection);
                businessLogics.systemEventsLM.computerConnection.change(navigator.getComputer().object, session, newConnection);
                businessLogics.systemEventsLM.connectionStatusConnection.change(businessLogics.systemEventsLM.connectionStatus.getObjectID("connectedConnection"), session, newConnection);
                businessLogics.systemEventsLM.connectTimeConnection.change(businessLogics.timeLM.currentDateTime.read(session), session, newConnection);
                businessLogics.systemEventsLM.remoteAddressConnection.change(navigator.getRemoteAddress(), session, newConnection);
                session.apply(businessLogics, stack);
            }
        }

        synchronized (navigators) {
            if (newConnection != null) {
                navigator.setConnection(new DataObject(newConnection.object, businessLogics.systemEventsLM.connection));
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
            try (DataSession session = dbManager.createSession()) {
                if (connection != null) {
                    businessLogics.systemEventsLM.connectionStatusConnection.change(businessLogics.systemEventsLM.connectionStatus.getObjectID("disconnectedConnection"), session, connection);
                } else
                    ServerLoggers.assertLog(false, "SHOULD NOT BE");
                session.apply(businessLogics, stack);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //    //логика EXPIRED навигаторов неактуальна, пока не работает механизм восстановления сессии
//    private synchronized void scheduleRemoveExpired() {
//        if (removeExpiredScheduled.compareAndSet(false, true)) {
//            executor.schedule(new Runnable() {
//                @Override
//                public void run() {
//                    removeNavigators(getStack(), NavigatorFilter.FALSE);
//                    removeExpiredScheduled.set(false);
//                }
//            }, 5, TimeUnit.SECONDS);
//        }
//    }

    public void updateEnvironmentProperty(CalcProperty property, ObjectValue value) throws SQLException {
        synchronized (navigators) { // могут быть закрывающиеся навигаторы, проверка с синхронизацией внутри вызова
            for (RemoteNavigator remoteNavigator : navigators)
                if (remoteNavigator != null)
                    remoteNavigator.updateEnvironmentProperty(property, value);
        }
    }

    public void pushNotificationCustomUser(DataObject connectionObject, EnvStackRunnable run) {
        synchronized (navigators) { // могут быть закрывающиеся навигаторы, проверка с синхронизацией внутри вызова
            boolean found = false;
            for (RemoteNavigator navigator : navigators) {
                if(navigator != null) {
                    try {
                        if (navigator.getConnection() != null && navigator.getConnection().equals(connectionObject)) {
                            if (!found) {
                                navigator.pushNotification(run);
                                found = true;
                            } else
                                ServerLoggers.assertLog(false, "Two RemoteNavigators with same connection");
                        }
                    } catch (RemoteException e) {
                        logger.error(getString("logics.server.remote.exception.on.push.action"), e);
                    }
                }
            }
        }
    }
}
