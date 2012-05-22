package platform.agent;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import platform.base.ClassUtils;

import java.io.IOException;

public class ServerAgentServerBootstrap {

    private static FileSystemXmlApplicationContext springContext;

    public static final String SETTINGS_PATH_KEY = "lsf.settings.path";
    public static final String DEFAULT_SETTINGS_PATH = "conf/settings.xml";

    public ServerAgentServerBootstrap() {
    }

    protected final static Logger logger = Logger.getLogger(ServerAgentServerBootstrap.class);

    private static ServerAgentServer serverAgentServer;

    public static void start() throws IOException {
        logger.info("Server Agent is starting...");

        ClassUtils.initRMICompressedSocketFactory();

        initSpringContext();

        try {
            serverAgentServer = (ServerAgentServer) springContext.getBean("serverAgentServer");
            logger.info("Server Agent has successfully started");
        } catch (BeanCreationException bce) {
            logger.info("Exception while starting Server Agent: ", bce);
        }

        if (springContext.containsBean("serverInstanceLocator")) {
            ServerInstanceLocator serverLocator = (ServerInstanceLocator) springContext.getBean("serverInstanceLocator");
            serverLocator.start();

            logger.info("Server instance locator successfully started");
        }
    }

    private static void initSpringContext() {
        String settingsPath = System.getProperty(SETTINGS_PATH_KEY);
        if (settingsPath == null) {
            settingsPath = DEFAULT_SETTINGS_PATH;
        }

        springContext = new FileSystemXmlApplicationContext(settingsPath);
    }

    public static void stop() {

        logger.info("Server is stopping...");

        if (serverAgentServer != null) {
            serverAgentServer.stop();
            serverAgentServer = null;
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

    public static void start(String[] args) throws IOException {
        start();
    }

    public static void stop(String[] args) {
        stop();
    }

    // -----------------------------
    // интерфейс для обычного старта
    // -----------------------------

    public static void main(String[] args) throws IOException {
        start();
    }


}
