package platform.server.logics;

import org.apache.log4j.Logger;
import platform.interop.DaemonThreadFactory;

import java.util.concurrent.*;

import static platform.server.logics.ServerResourceBundle.getString;

public class RestartController {
    private final static Logger logger = Logger.getLogger(RestartController.class);

    private static final int restartDelayMinutes = 15;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());

    private final BusinessLogics<?> BL;
    private Future restartFuture;
    private NavigatorsController navigatorsController;

    public RestartController(BusinessLogics<?> BL) {
        this.BL = BL;
        this.navigatorsController = BL.navigatorsController;
    }

    public synchronized void scheduleRestart() {
        if (restartFuture != null) {
            return;
        }

        logger.info(getString("logics.server.initiated.server.stopping"));
        try {
            restartFuture = scheduler.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    boolean canRestart = navigatorsController.notifyServerRestart();
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
        scheduler.schedule(new Runnable() {
            public void run() {
                logger.info(getString("logics.server.server.stopping"));
                System.exit(0);
            }
        }, 5, TimeUnit.SECONDS);
    }

    public synchronized boolean isPendingRestart() {
        return restartFuture != null && !restartFuture.isDone();
    }

    public synchronized void cancelRestart() {
        if (restartFuture == null) {
            return;
        }

        logger.info(getString("logics.server.stopping.canceled"));
        restartFuture.cancel(false);

        restartFuture = null;

        scheduler.submit(new Runnable() {
            public void run() {
                navigatorsController.notifyServerRestartCanceled();
            }
        });
    }

    public synchronized void forcedRestartIfPending() {
        if (isPendingRestart()) {
            logger.info(getString("logics.server.all.clients.disconnected.server.will.be.stopped"));
            doRestart();
        }
    }
}
