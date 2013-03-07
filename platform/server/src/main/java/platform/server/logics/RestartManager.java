package platform.server.logics;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import platform.interop.DaemonThreadFactory;
import platform.server.classes.LogicalClass;
import platform.server.logics.property.CalcProperty;

import java.sql.SQLException;
import java.util.concurrent.*;

import static platform.server.logics.ServerResourceBundle.getString;

public class RestartManager implements InitializingBean {
    private static final Logger logger = Logger.getLogger(RestartManager.class);

    private static final int restartDelayMinutes = 15;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());

    private NavigatorsManager navigatorsManager;
    private BusinessLogics businessLogics;

    private Future restartFuture;

    public RestartManager() {
    }

    public void setBusinessLogics(BusinessLogics businessLogics) {
        this.businessLogics = businessLogics;
    }

    public void setNavigatorsManager(NavigatorsManager navigatorsManager) {
        this.navigatorsManager = navigatorsManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(navigatorsManager, "navigatorsManager must be specified");
    }

    public synchronized void scheduleRestart() throws SQLException {
        if (restartFuture != null) {
            return;
        }

        logger.info(getString("logics.server.initiated.server.stopping"));
        try {
            restartFuture = scheduler.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    boolean canRestart = navigatorsManager.notifyServerRestart();
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
        updateRestartProperty();
    }

    private synchronized void doRestart() {
        //в отдельном потоке, чтобы вернуть управление в точку вызова,
        //чтобы удалённый клиент продолжил выполнение
        scheduler.schedule(new Runnable() {
            public void run() {
                logger.info(getString("logics.server.server.stopping"));
                BusinessLogicsBootstrap.stop();
            }
        }, 5, TimeUnit.SECONDS);
    }

    public synchronized boolean isPendingRestart() {
        return restartFuture != null && !restartFuture.isDone();
    }

    public synchronized void cancelRestart() throws SQLException {
        if (restartFuture == null) {
            return;
        }

        logger.info(getString("logics.server.stopping.canceled"));
        restartFuture.cancel(false);

        restartFuture = null;

        scheduler.submit(new Runnable() {
            public void run() {
                navigatorsManager.notifyServerRestartCanceled();
            }
        });

        updateRestartProperty();
    }

    public synchronized void forcedRestartIfPending() {
        if (isPendingRestart()) {
            logger.info(getString("logics.server.all.clients.disconnected.server.will.be.stopped"));
            doRestart();
        }
    }

    public void updateRestartProperty() throws SQLException {
        Boolean isRestarting = isPendingRestart() ? Boolean.TRUE : null;
        navigatorsManager.updateEnvironmentProperty(
                (CalcProperty) businessLogics.serviceLM.isServerRestarting.property,
                ObjectValue.getValue(isRestarting, LogicalClass.instance)
        );
    }
}
