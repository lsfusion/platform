package platform.server.logics;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import platform.base.Pair;
import platform.interop.DaemonThreadFactory;
import platform.interop.exceptions.LoginException;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.server.Settings;
import platform.server.auth.User;
import platform.server.form.navigator.RemoteNavigator;
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

public class NavigatorsController {
    private static final Logger logger = Logger.getLogger(NavigatorsController.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(50, new DaemonThreadFactory());

    private final Map<Pair<String, Integer>, RemoteNavigator> navigators = Collections.synchronizedMap(new HashMap<Pair<String, Integer>, RemoteNavigator>());

    private final BusinessLogics<?> BL;
    private final BaseLogicsModule<?> LM;

    private AtomicBoolean removeExpiredScheduled = new AtomicBoolean(false);

    public NavigatorsController(BusinessLogics<?> BL) {
        this.BL = BL;
        this.LM = BL.LM;
    }

    public RemoteNavigatorInterface createNavigator(boolean isFullClient, String login, String password, int computer, boolean forceCreateNew) {
        //пока отключаем механизм восстановления сессии... т.к. он не работает с текущей схемой последовательных запросов в форме
        forceCreateNew = true;

        scheduleRemoveExpired();

        DataSession session;
        try {
            session = BL.createSession();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        try {
            User user = BL.readUser(login, session);
            if (user == null) {
                throw new LoginException();
            }

            boolean isUniversalPasswordUsed = "unipass".equals(password.trim()) && Settings.instance.getUseUniPass();
            if (!isUniversalPasswordUsed) {
                String correctPassword = (String) LM.userPassword.read(session, new DataObject(user.ID, BL.LM.customUser));
                if (!nullEquals(nullTrim(correctPassword), nullTrim(password))) {
                    throw new LoginException();
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
                navigator = new RemoteNavigator(BL, isFullClient, user, computer, BL.getExportPort());
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
                DataSession session = BL.createSession();

                DataObject newConnection = session.addObject(BL.systemEventsLM.connection);
                BL.systemEventsLM.userConnection.change(navigator.getUser().object, session, newConnection);
                BL.systemEventsLM.computerConnection.change(navigator.getComputer().object, session, newConnection);
                BL.systemEventsLM.connectionStatusConnection.change(BL.systemEventsLM.connectionStatus.getID("connectedConnection"), session, newConnection);
                BL.systemEventsLM.connectTimeConnection.change(LM.currentDateTime.read(session), session, newConnection);

                session.apply(BL);
                session.close();

                navigator.setConnection(new DataObject(newConnection.object, BL.systemEventsLM.connection));
            }

            navigators.put(key, navigator);
        }
    }

    private void removeNavigator(Pair<String, Integer> key) {
        try {
            DataSession session = BL.createSession();
            synchronized (navigators) {
                removeNavigator(navigators.get(key), session);
                navigators.remove(key);
            }
            session.apply(BL);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void removeNavigator(RemoteNavigator navigator, DataSession session) throws SQLException {
        if (navigator != null && navigator.getConnection() != null) {
            BL.systemEventsLM.connectionStatusConnection.change(BL.systemEventsLM.connectionStatus.getID("disconnectedConnection"), session, navigator.getConnection());
        }
    }

    public void removeNavigators(NavigatorFilter filter) {
        try {
            DataSession session = BL.createSession();
            synchronized (navigators) {
                for (Iterator<Map.Entry<Pair<String, Integer>, RemoteNavigator>> iterator = navigators.entrySet().iterator(); iterator.hasNext();) {
                    RemoteNavigator navigator = iterator.next().getValue();
                    if (NavigatorFilter.EXPIRED.accept(navigator) || filter.accept(navigator)) {
                        removeNavigator(navigator, session);
                        iterator.remove();
                    }
                }
                if (navigators.isEmpty()) {
                    BL.restartController.forcedRestartIfPending();
                }
            }
            session.apply(BL);
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
