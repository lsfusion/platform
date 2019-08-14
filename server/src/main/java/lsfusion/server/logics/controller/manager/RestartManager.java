package lsfusion.server.logics.controller.manager;

import lsfusion.base.DaemonThreadFactory;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.BusinessLogicsBootstrap;
import lsfusion.server.logics.navigator.controller.manager.NavigatorsManager;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

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
    public void afterPropertiesSet() {
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(navigatorsManager, "navigatorsManager must be specified");
    }

    public synchronized void scheduleRestart() {
        if (restartFuture != null) {
            return;
        }

        logger.info("Server Stopping initiated");
        try {
            restartFuture = scheduler.scheduleAtFixedRate(this::doRestart, restartDelayMinutes, restartDelayMinutes, TimeUnit.MINUTES);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private synchronized void doRestart() {
        //в отдельном потоке, чтобы вернуть управление в точку вызова,
        //чтобы удалённый клиент продолжил выполнение
        scheduler.schedule(() -> {
            logger.info("Server stopping...");
            BusinessLogicsBootstrap.stop();
        }, 5, TimeUnit.SECONDS);
    }

    public synchronized boolean isPendingRestart() {
        return pendingRestart;
    }

    public void setPendingRestart(boolean pendingRestart) {
        this.pendingRestart = pendingRestart;
    }

    public synchronized void cancelRestart() {
        if (restartFuture == null) {
            return;
        }

        logger.info("Server stopping canceled.");
        restartFuture.cancel(false);

        restartFuture = null;
    }

    public synchronized void forcedRestartIfPending() {
        if (isPendingRestart()) {
            logger.info("All clients were disconnected, so the server will be stopped.");
            doRestart();
        }
    }
}
