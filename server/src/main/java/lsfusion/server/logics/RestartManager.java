package lsfusion.server.logics;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import lsfusion.interop.DaemonThreadFactory;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.logics.property.CalcProperty;

import java.sql.SQLException;
import java.util.concurrent.*;

public class RestartManager implements InitializingBean {
    private static final Logger logger = ServerLoggers.systemLogger;

    private static final int restartDelayMinutes = 5;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("restart-daemon"));

    private NavigatorsManager navigatorsManager;
    private BusinessLogics businessLogics;

    private Future restartFuture;
    private boolean pendingRestart = false; //changes in service.lsf

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

        logger.info("Server Stopping initiated");
        try {
            restartFuture = scheduler.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    doRestart();
                }
            }, restartDelayMinutes, restartDelayMinutes, TimeUnit.MINUTES);
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
                logger.info("Server stopping...");
                BusinessLogicsBootstrap.stop();
            }
        }, 5, TimeUnit.SECONDS);
    }

    public synchronized boolean isPendingRestart() {
        return pendingRestart;
    }

    public void setPendingRestart(boolean pendingRestart) {
        this.pendingRestart = pendingRestart;
    }

    public synchronized void cancelRestart() throws SQLException {
        if (restartFuture == null) {
            return;
        }

        logger.info("Server stopping canceled.");
        restartFuture.cancel(false);

        restartFuture = null;

        updateRestartProperty();
    }

    public synchronized void forcedRestartIfPending() {
        if (isPendingRestart()) {
            logger.info("All clients were disconnected, so the server will be stopped.");
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
