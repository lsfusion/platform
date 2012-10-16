package platform.server.logics;

import org.apache.log4j.Logger;
import platform.interop.DaemonThreadFactory;
import platform.server.form.navigator.RemoteNavigator;

import java.rmi.RemoteException;
import java.util.concurrent.*;

import static platform.server.logics.ServerResourceBundle.getString;

class RestartController {
    private final static Logger logger = Logger.getLogger(RestartController.class);

    private static final int restartDelayMinutes = 15;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());

    private final BusinessLogics<?> BL;
    private Future task;

    public RestartController(BusinessLogics<?> BL) {
        this.BL = BL;
    }

    public synchronized void initRestart() {
        if (task != null) {
            return;
        }

        logger.info(getString("logics.server.initiated.server.stopping"));
        try {
            task = scheduler.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    boolean canRestart = true;
                    synchronized (BL.navigators) {
                        for (RemoteNavigator remoteNavigator : BL.navigators.values()) {
                            if (!remoteNavigator.isRestartAllowed()) {
                                canRestart = false;
                                try {
                                    remoteNavigator.notifyServerRestart();
                                } catch (RemoteException e) {
                                    logger.error(getString("logics.server.remote.exception.on.questioning.client.for.stopping"), e);
                                }
                            }
                        }
                    }
                    if (canRestart) {
                        doRestart();
                    } else {
                        logger.info(getString("logics.server.some.clients.prohibited.server.stopping"));
                    }
                }
            }, 0, restartDelayMinutes, TimeUnit.MINUTES);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private synchronized void doRestart() {
        //в отдельном потоке, чтобы вернуть управление в точку вызова,
        //чтобы удалённый клиент продолжил выполнение
        scheduler.submit(new Runnable() {
            public void run() {
                logger.info(getString("logics.server.server.stopping"));
                System.exit(0);
            }
        });
    }

    public synchronized boolean isPendingRestart() {
        return task != null && !task.isDone();
    }

    public synchronized void cancelRestart() {
        if (task == null) {
            return;
        }

        logger.info(getString("logics.server.stopping.canceled"));
        task.cancel(false);

        task = null;

        scheduler.submit(new Runnable() {
            public void run() {
                synchronized (BL.navigators) {
                    for (RemoteNavigator remoteNavigator : BL.navigators.values()) {
                        try {
                            remoteNavigator.notifyServerRestartCanceled();
                        } catch (RemoteException e) {
                            logger.error(getString("logics.server.remote.exception.on.questioning.client.for.stopping"), e);
                        }
                    }
                }
            }
        });
    }

    public synchronized void forcedRestartIfAllowed() {
        if (isPendingRestart()) {
            synchronized (BL.navigators) {
                if (BL.navigators.size() == 0) {
                    logger.info(getString("logics.server.all.clients.disconnected.server.will.be.stopped"));
                    doRestart();
                }
            }
        }
    }
}
