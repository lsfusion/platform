package retail.equipment;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import platform.base.ClassUtils;

import java.io.IOException;

public class EquipmentServerBootstrap {

    private static FileSystemXmlApplicationContext springContext;

    public static final String SETTINGS_PATH_KEY = "lsf.settings.path";
    public static final String DEFAULT_SETTINGS_PATH = "conf/settings.xml";

    public EquipmentServerBootstrap() {
    }

    protected final static Logger logger = Logger.getLogger(EquipmentServerBootstrap.class);

    private static EquipmentServer equ;

    public static void start() throws IOException {
        logger.info("Server is starting...");

        ClassUtils.initRMICompressedSocketFactory();

        initSpringContext();

        try {
            equ = (EquipmentServer) springContext.getBean("equipmentServer");
            logger.info("Server has successfully started");
        } catch (BeanCreationException bce) {
            logger.info("Exception while starting equipment server: ", bce);
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

        if (equ != null) {
            equ.stop();
            equ = null;
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
