package platform.server.logics;

import org.apache.log4j.Logger;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import platform.base.SystemUtils;
import platform.base.col.lru.LRUCache;
import platform.interop.form.RemoteFormInterface;
import platform.server.Settings;
import platform.server.SystemProperties;

import java.util.HashMap;

public class BusinessLogicsBootstrap {
    private static final Logger logger = Logger.getLogger(BusinessLogicsBootstrap.class);

    private static FileSystemXmlApplicationContext springContext;

    private static LogicsInstance logicsInstance;

    public static void start() {
        SystemProperties.enableMailEncodeFileName();

        logger.info("Server is starting...");

//        initLRUCaches();

        boolean instanceCreated = false;
        try {
            springContext = new FileSystemXmlApplicationContext(SystemProperties.settingsPath);
            logicsInstance = (LogicsInstance) springContext.getBean("logicsInstance");
            instanceCreated = true;
        } catch (Throwable t) {
            logger.error("Error creating logics instance: ", t);
        }

        if (instanceCreated) {
            try {
                logicsInstance.start();
                logger.info("Server has successfully started");
            } catch (Throwable e) {
                logger.info("Error starting server, server will be stopped.");
                stop();
            }
        }
    }

    private static void initLRUCaches() {
        Settings settings = logicsInstance.getSettings();
        LRUCache.init(new int[]{settings.getLRUOftenCleanPeriod(), settings.getLRURareCleanPeriod()},
                      new int[]{settings.getLRUOftenExpireSecond(), settings.getLRURareExpireSecond()},
                      new int[]{settings.getLRUOftenProceedBucket(), settings.getLRURareProceedBucket()});
    }

    public static void stop() {
        logger.info("Server is stopping...");

        logicsInstance.stop();

        // иногда не удаётся нормально убрать все RMI ссылки,
        // поэтому убиваем RMI поток сами, а то зависает
        SystemUtils.killRmiThread();

        logger.info("Server has stopped...");
   }

    // -------------------------------
    // интерфейс для старта через jsvc
    // -------------------------------

    public static void init(String[] args) {
    }

    public static void destroy() {
    }

    // ----------------------------------
    // интерфейс для старта через procrun
    // ----------------------------------

    public static void start(String[] args) {
        start();
    }

    public static void stop(String[] args) {
        stop();
    }

    // -----------------------------
    // интерфейс для обычного старта
    // -----------------------------

    public static void main(String[] args) {
        start();
    }
}
