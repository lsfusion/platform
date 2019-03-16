package lsfusion.server.logics;

import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import lsfusion.base.file.IOUtils;
import lsfusion.base.remote.RMIUtils;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.logging.ServerLoggers;
import org.apache.log4j.Logger;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BusinessLogicsBootstrap {
    private static final Logger logger = ServerLoggers.startLogger;

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
            AbstractXmlApplicationContext springContext = new ClassPathXmlApplicationContext(getSettingsPath());
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

                String version = BaseUtils.getPlatformVersion();
                if(version != null) {
                    logger.info("Desktop Client is available at http://download.lsfusion.org/" + version + "/lsfusion-client-" + version + ".jnlp");
                }
                logger.info("Server has successfully started in " + (System.currentTimeMillis() - startTime) + " ms.");
            } catch (Throwable e) {
                logger.info("Error starting server, server will be stopped.");
                stop();
            }
        }
    }

    private static String getSettingsPath() throws IOException {
        String settingsPath = null;
        InputStream settingsStream = BusinessLogicsBootstrap.class.getResourceAsStream("/lsfusion.properties");
        if (settingsStream != null) {
            Scanner scanner = new Scanner(IOUtils.readStreamToString(settingsStream));
            while (scanner.hasNextLine()) {
                Pattern p = Pattern.compile("logics\\.lsfusionXMLPath=(.*)");
                Matcher m = p.matcher(scanner.nextLine());
                if (m.matches()) {
                    settingsPath = m.group(1);
                }
            }
        }
        return settingsPath != null ? settingsPath : "lsfusion-bootstrap.xml";
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
