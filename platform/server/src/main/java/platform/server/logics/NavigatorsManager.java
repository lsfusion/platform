package platform.server.logics;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import platform.base.BaseUtils;
import platform.base.Pair;
import platform.interop.exceptions.LoginException;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.UserInfo;
import platform.server.Settings;
import platform.server.auth.User;
import platform.server.context.ContextAwareDaemonThreadFactory;
import platform.server.form.navigator.RemoteNavigator;
import platform.server.lifecycle.LifecycleAdapter;
import platform.server.lifecycle.LifecycleEvent;
import platform.server.logics.property.CalcProperty;
import platform.server.session.DataSession;

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

import static platform.base.BaseUtils.nullEquals;
import static platform.base.BaseUtils.nullTrim;
import static platform.server.logics.ServerResourceBundle.getString;

public class NavigatorsManager extends LifecycleAdapter implements InitializingBean {
    private static final Logger logger = Logger.getLogger(NavigatorsManager.class);

    //время жизни неиспользуемого навигатора - 3 часа по умолчанию
    public static final long MAX_FREE_NAVIGATOR_LIFE_TIME = Long.parseLong(System.getProperty("platform.server.navigatorMaxLifeTime", Long.toString(3L * 3600L * 1000L)));

    private LogicsInstance logicsInstance;

    private BusinessLogics<?> businessLogics;

    private BaseLogicsModule<?> baseLM;

    private RestartManager restartManager;

    private SecurityManager securityManager;

    private RMIManager rmiManager;

    private DBManager dbManager;

    private ScheduledExecutorService scheduler;

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
        scheduler = Executors.newScheduledThreadPool(50, new ContextAwareDaemonThreadFactory(logicsInstance.getContext()));
    }

    public RemoteNavigatorInterface createNavigator(boolean isFullClient, String login, String password, int computer, String remoteAddress, boolean forceCreateNew) {
        //пока отключаем механизм восстановления сессии... т.к. он не работает с текущей схемой последовательных запросов в форме
        forceCreateNew = true;

        scheduleRemoveExpired();

        DataSession session;
        try {
            session = dbManager.createSession();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        try {
            User user = securityManager.readUser(login, session);
            if (user == null) {
                throw new LoginException();
            }

            boolean isUniversalPasswordUsed = "unipass".equals(password.trim()) && Settings.get().getUseUniPass();
            if (!isUniversalPasswordUsed) {
                String hashPassword = (String) businessLogics.authenticationLM.sha256PasswordCustomUser.read(session, new DataObject(user.ID, businessLogics.authenticationLM.customUser));
                if (hashPassword != null) {
                    if (!hashPassword.trim().equals(BaseUtils.calculateBase64Hash("SHA-256", nullTrim(password), UserInfo.salt))) {
                        throw new LoginException();
                    }
                } else {
                    String correctPassword = (String) businessLogics.authenticationLM.passwordCustomUser.read(session, new DataObject(user.ID, businessLogics.authenticationLM.customUser));
                    if (!nullEquals(nullTrim(correctPassword), nullTrim(password))) {
                        throw new LoginException();
                    }
                }
            }

            Pair<String, Integer> loginKey = new Pair<String, Integer>(login, computer);
            RemoteNavigator navigator = forceCreateNew ? null : navigators.get(loginKey);

            if (navigator != null) {
                if (navigator.isFullClient() != isFullClient) {
                    //создаём новый навигатор, если поменялся тип клиента
                    navigator = null;
                } else {
                    navigator.invalidate();
                    if (navigator.isBusy()) {
                        navigator = null;
                        removeNavigator(loginKey);
                    }
                }
            }

            if (navigator == null) {
                navigator = new RemoteNavigator(logicsInstance, isFullClient, remoteAddress, user, computer, rmiManager.getExportPort());
                addNavigator(loginKey, navigator, isUniversalPasswordUsed);
            }

            return navigator;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            try {
                session.close();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private void addNavigator(Pair<String, Integer> key, RemoteNavigator navigator, boolean skipLogging) throws SQLException {
        synchronized (navigators) {

            if (!skipLogging) {
                DataSession session = dbManager.createSession();

                DataObject newConnection = session.addObject(businessLogics.systemEventsLM.connection);
                businessLogics.systemEventsLM.userConnection.change(navigator.getUser().object, session, newConnection);
                businessLogics.systemEventsLM.computerConnection.change(navigator.getComputer().object, session, newConnection);
                businessLogics.systemEventsLM.connectionStatusConnection.change(businessLogics.systemEventsLM.connectionStatus.getObjectID("connectedConnection"), session, newConnection);
                businessLogics.systemEventsLM.connectTimeConnection.change(baseLM.currentDateTime.read(session), session, newConnection);
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

    private void removeNavigator(RemoteNavigator navigator, DataSession session) throws SQLException {
        if (navigator != null && navigator.getConnection() != null) {
            businessLogics.systemEventsLM.connectionStatusConnection.change(businessLogics.systemEventsLM.connectionStatus.getObjectID("disconnectedConnection"), session, navigator.getConnection());
        }
    }

    public void removeNavigators(NavigatorFilter filter) {
        try {
            DataSession session = dbManager.createSession();
            synchronized (navigators) {
                for (Iterator<Map.Entry<Pair<String, Integer>, RemoteNavigator>> iterator = navigators.entrySet().iterator(); iterator.hasNext();) {
                    RemoteNavigator navigator = iterator.next().getValue();
                    if (NavigatorFilter.EXPIRED.accept(navigator) || filter.accept(navigator)) {
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

    private synchronized void scheduleRemoveExpired() {
        if (removeExpiredScheduled.compareAndSet(false, true)) {
            scheduler.schedule(new Runnable() {
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

    public void cutOffConnection(Pair<String, Integer> key) {
        try {
            final RemoteNavigator navigator = navigators.get(key);
            if (navigator != null) {
                navigator.getClientCallBack().cutOff();
                removeNavigator(key);

                if (navigator.isBusy()) {
                    Thread.sleep(navigator.getUpdateTime() * 3); //ожидаем, пока пройдёт пинг и убъётся сокет. затем грохаем поток. чтобы не словить ThreadDeath на клиенте.
                    navigator.killThreads();
                }
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
