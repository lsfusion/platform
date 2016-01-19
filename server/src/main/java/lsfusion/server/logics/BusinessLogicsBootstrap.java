package lsfusion.server.logics;

import lsfusion.base.SystemUtils;
import lsfusion.interop.remote.RMIUtils;
import lsfusion.server.ServerLoggers;
import lsfusion.server.SystemProperties;
import org.apache.log4j.Logger;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BusinessLogicsBootstrap {
    private static final Logger logger = ServerLoggers.systemLogger;

    private static LogicsInstance logicsInstance;

    private static volatile boolean stopped = true;

    public static void start() {
        SystemProperties.enableMailEncodeFileName();
        // делаем, чтобы сборщик мусора срабатывал каждую минуту - для удаления ненужных connection'ов
        SystemProperties.setDGCParams();

        long startTime = System.currentTimeMillis();
        logger.info("Server is starting...");

//        initLRUCaches();

        boolean instanceCreated = false;
        try {
            AbstractXmlApplicationContext springContext = new ClassPathXmlApplicationContext(SystemProperties.settingsPath);
            logicsInstance = (LogicsInstance) springContext.getBean("logicsInstance");
            instanceCreated = true;
        } catch (Throwable t) {
            logger.error("Error creating logics instance: ", t);
        }

        if (instanceCreated) {
            try {
                stopped = false;

                logicsInstance.start();

                registerShutdownHook();

                if(DBManager.explicitMigrate)
                    logger.info("Server needs to be started once again...");

                logger.info("Server has successfully started in " + (System.currentTimeMillis() - startTime) + " ms.");
            } catch (Throwable e) {
                logger.info("Error starting server, server will be stopped.");
                stop();
            }
        }
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (!stopped) {
                    logger.info("Executing shutdown hook.");
                    BusinessLogicsBootstrap.stop();
                }
            }
        });
    }

/*    private static void initLRUCaches() {
        Settings settings = logicsInstance.getSettings();
        LRUCache.init(new int[]{settings.getLRUOftenCleanPeriod(), settings.getLRURareCleanPeriod()},
                      new int[]{settings.getLRUOftenExpireSecond(), settings.getLRURareExpireSecond()},
                      new int[]{settings.getLRUOftenProceedBucket(), settings.getLRURareProceedBucket()});
    }*/

    public synchronized static void stop() {
        if (!stopped) {
            logger.info("Server is stopping...");

            try {
                logicsInstance.stop();
            } catch (Throwable ignored) {
            }

            stopped = true;

            // иногда не удаётся нормально убрать все RMI ссылки,
            // поэтому убиваем RMI поток сами, а то зависает
            RMIUtils.killRmiThread();

            logger.info("Server has stopped...");

            // форсируем выход в отдельном потоке
            final Thread closer = new Thread("Closing thread...") {
                @Override
                public void run() {
                    //убиваемся, если через 5 секунд ещё не вышли
                    SystemUtils.sleep(5000);
                    System.exit(0);
                }
            };
            closer.setDaemon(true);
            closer.start();
        }
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
