package lsfusion.server.logics;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.server.auth.User;
import lsfusion.server.context.ContextAwareDaemonThreadFactory;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.navigator.RemoteNavigator;
import lsfusion.server.lifecycle.LifecycleAdapter;
import lsfusion.server.lifecycle.LifecycleEvent;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.session.DataSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class NavigatorsManager extends LifecycleAdapter implements InitializingBean {
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

    private ScheduledExecutorService executor;

    private final Map<Pair<String, Integer>, RemoteNavigator> navigators = Collections.synchronizedMap(new HashMap<Pair<String, Integer>, RemoteNavigator>());

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
        executor = Executors.newSingleThreadScheduledExecutor(new ContextAwareDaemonThreadFactory(logicsInstance.getContext(), "navigator-manager-daemon"));
    }

    public RemoteNavigatorInterface createNavigator(boolean isFullClient, String login, String password, int computer, String remoteAddress, boolean reuseSession) {
        //пока отключаем механизм восстановления сессии... т.к. он не работает с текущей схемой последовательных запросов в форме
        reuseSession = false;

        //логика EXPIRED навигаторов неактуальна, пока не работает механизм восстановления сессии
//        scheduleRemoveExpired();

        DataSession session;
        try {
            session = dbManager.createSession();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        try {
            User user = securityManager.authenticateUser(session, login, password);

            Pair<String, Integer> loginKey = new Pair<String, Integer>(login, computer);

            if (reuseSession) {
                RemoteNavigator navigator = navigators.get(loginKey);
                if (navigator != null) {
                    navigator.disconnect();
                    navigator.unexportAndClean();
                    removeNavigator(loginKey);
                }
            }

            RemoteNavigator navigator = new RemoteNavigator(logicsInstance, isFullClient, remoteAddress, user, computer, rmiManager.getExportPort(), session);
            addNavigator(loginKey, navigator, securityManager.isUniversalPassword(password));

            return navigator;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            try {
                session.apply(businessLogics);
                session.close();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private void addNavigator(Pair<String, Integer> key, RemoteNavigator navigator, boolean skipLogging) throws SQLException, SQLHandledException {
        synchronized (navigators) {

            if (!skipLogging) {
                DataSession session = dbManager.createSession();

                DataObject newConnection = session.addObject(businessLogics.systemEventsLM.connection);
                businessLogics.systemEventsLM.userConnection.change(navigator.getUser().object, session, newConnection);
                businessLogics.systemEventsLM.computerConnection.change(navigator.getComputer().object, session, newConnection);
                businessLogics.systemEventsLM.connectionStatusConnection.change(businessLogics.systemEventsLM.connectionStatus.getObjectID("connectedConnection"), session, newConnection);
                businessLogics.systemEventsLM.connectTimeConnection.change(businessLogics.timeLM.currentDateTime.read(session), session, newConnection);
                businessLogics.systemEventsLM.remoteAddressConnection.change(navigator.getRemoteAddress(), session, newConnection);
                session.apply(businessLogics);
                session.close();

                navigator.setConnection(new DataObject(newConnection.object, businessLogics.systemEventsLM.connection));
            }

            navigators.put(key, navigator);
        }
    }

    private void removeNavigator(Pair<String, Integer> key) {
        try {
            DataSession session = dbManager.createSession();
            synchronized (navigators) {
                removeNavigator(navigators.get(key), session);
                navigators.remove(key);
            }
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void removeNavigator(RemoteNavigator navigator, DataSession session) throws SQLException, SQLHandledException {
        if (navigator != null && navigator.getConnection() != null) {
            businessLogics.systemEventsLM.connectionStatusConnection.change(businessLogics.systemEventsLM.connectionStatus.getObjectID("disconnectedConnection"), session, navigator.getConnection());
        }
    }

    public void navigatorClosed(RemoteNavigator navigator) {
        removeNavigators(NavigatorFilter.single(navigator));
    }

    private void removeNavigators(NavigatorFilter filter) {
        try {
            DataSession session = dbManager.createSession();
            synchronized (navigators) {
                for (Iterator<Map.Entry<Pair<String, Integer>, RemoteNavigator>> iterator = navigators.entrySet().iterator(); iterator.hasNext(); ) {
                    RemoteNavigator navigator = iterator.next().getValue();
                    //логика EXPIRED навигаторов неактуальна, пока не работает механизм восстановления сессии
//                    if (NavigatorFilter.EXPIRED.accept(navigator) || filter.accept(navigator)) {
                    if (filter.accept(navigator)) {
                        removeNavigator(navigator, session);
                        iterator.remove();
                    }
                }
                if (navigators.isEmpty()) {
                    restartManager.forcedRestartIfPending();
                }
            }
            session.apply(businessLogics);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //логика EXPIRED навигаторов неактуальна, пока не работает механизм восстановления сессии
    private synchronized void scheduleRemoveExpired() {
        if (removeExpiredScheduled.compareAndSet(false, true)) {
            executor.schedule(new Runnable() {
                @Override
                public void run() {
                    removeNavigators(NavigatorFilter.FALSE);
                    removeExpiredScheduled.set(false);
                }
            }, 5, TimeUnit.SECONDS);
        }
    }

    public void updateEnvironmentProperty(CalcProperty property, ObjectValue value) throws SQLException {
        synchronized (navigators) {
            for (RemoteNavigator remoteNavigator : navigators.values()) {
                remoteNavigator.updateEnvironmentProperty(property, value);
            }
        }
    }

    public void forceDisconnect(Pair<String, Integer> key) {
        final RemoteNavigator navigator = navigators.get(key);
        if (navigator != null) {
            navigator.disconnect();

            removeNavigator(key);
            navigator.unexportAndCleanLater();
        }
    }

    public boolean notifyServerRestart() {
        synchronized (navigators) {
            boolean canRestart = true;
            for (RemoteNavigator remoteNavigator : navigators.values()) {
                if (!remoteNavigator.isRestartAllowed()) {
                    canRestart = false;
                    try {
                        remoteNavigator.notifyServerRestart();
                    } catch (RemoteException e) {
                        logger.error(getString("logics.server.remote.exception.on.questioning.client.for.stopping"), e);
                    }
                }
            }
            return canRestart;
        }
    }

    public void notifyServerRestartCanceled() {
        synchronized (navigators) {
            for (RemoteNavigator remoteNavigator : navigators.values()) {
                try {
                    remoteNavigator.notifyServerRestartCanceled();
                } catch (RemoteException e) {
                    logger.error(getString("logics.server.remote.exception.on.questioning.client.for.stopping"), e);
                }
            }
        }
    }
}
