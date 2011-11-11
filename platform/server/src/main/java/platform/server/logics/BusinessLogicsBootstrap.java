package platform.server.logics;

import net.sf.jasperreports.engine.JRException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import platform.base.ExceptionUtils;
import platform.interop.remote.ServerSocketFactory;
import platform.server.Settings;
import platform.server.lifecycle.LifecycleManager;
import platform.server.net.ServerInstanceLocator;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;
import java.sql.SQLException;

public class BusinessLogicsBootstrap {
    private static FileSystemXmlApplicationContext springContext;

    private static LifecycleManager lifecycle;

    public static final String SETTINGS_PATH_KEY = "lsf.settings.path";
    public static final String DEFAULT_SETTINGS_PATH = "conf/settings.xml";
    public static final String PLATFORM_SERVER_ISDEBUG = "platform.server.isdebug";

    public BusinessLogicsBootstrap() {
    }

    protected final static Logger logger = Logger.getLogger(BusinessLogicsBootstrap.class);

    private static Boolean stopped = false;
    private static final Object serviceMonitor = new Object();

    private static BusinessLogics BL;
    private static Registry registry;

    private static void initRMISocketFactory() throws IOException {
        if (RMISocketFactory.getSocketFactory() == null) {
            RMISocketFactory.setFailureHandler(new RMIFailureHandler() {
                public boolean failure(Exception ex) {
                    logger.error("Ошибка RMI: ", ex);
                    return true;
                }
            });

            RMISocketFactory.setSocketFactory(new ServerSocketFactory());
        }
    }

    public static void start() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, JRException {

        // делаем, чтобы сборщик мусора срабатывал каждую минуту - для удаления ненужных connection'ов

        if (System.getProperty("sun.rmi.dgc.server.gcInterval") == null) {
            System.setProperty("sun.rmi.dgc.server.gcInterval", "600000");
        }

//        String isDebug = System.getProperty(PLATFORM_SERVER_ISDEBUG);
//        if (isDebug == null || !isDebug.equals("true")) {
//            System.setProperty("java.rmi.dgc.leaseValue", "120000");
//        }

        System.setProperty("mail.mime.encodefilename", "true");

        logger.info("Server is starting...");

        stopped = false;

        initRMISocketFactory();

        initSpringContext();

        initLifecycleManager();

        lifecycle.fireStarting();

        boolean blCreated = true;
        try {
            BL = (BusinessLogics) springContext.getBean("businessLogics");
        } catch (BeanCreationException bce) {
            logger.info("Exception while creating business logic: ", bce);
            blCreated = false;

            lifecycle.fireError("Error, while creating bl: " + ExceptionUtils.getNonSpringCause(bce).getMessage());
        }

        if (blCreated) {
            lifecycle.fireBlCreated(BL);

            initRMIRegistry();

            initServiceLocator();

            logger.info("Server has successfully started");
            lifecycle.fireStarted();

            synchronized (serviceMonitor) {
                while (!stopped) {
                    try {
                        serviceMonitor.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            logger.info("Server has successfully stopped");
        } else {
            lifecycle.fireStopping();
            killRmiThread();
            logger.info("Server has stopped");
        }
        lifecycle.fireStopped();
    }

    private static void initRMIRegistry() throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException {
        registry = LocateRegistry.createRegistry(BL.getExportPort());
        registry.rebind("AppTerminal", new ApplicationTerminalImpl(BL.getExportPort()));
        registry.rebind("BusinessLogicsLoader", new BusinessLogicsLoader(BL));
    }

    private static void initSpringContext() {
        String settingsPath = System.getProperty(SETTINGS_PATH_KEY);
        if (settingsPath == null) {
            settingsPath = DEFAULT_SETTINGS_PATH;
        }

        springContext = new FileSystemXmlApplicationContext(settingsPath);
        if (springContext.containsBean("settings")) {
            Settings.instance = (Settings) springContext.getBean("settings");
        } else {
            Settings.instance = new Settings();
        }
    }

    private static void initServiceLocator() {
        if (springContext.containsBean("serverInstanceLocator")) {
            ServerInstanceLocator serverLocator = (ServerInstanceLocator) springContext.getBean("serverInstanceLocator");
            serverLocator.start();

            logger.info("Server instance locator successfully started");
        }
    }

    private static void initLifecycleManager() {
        if (springContext.containsBean("lifecycleManager")) {
            lifecycle = (LifecycleManager) springContext.getBean("lifecycleManager");
        } else {
            lifecycle = new LifecycleManager();
        }
    }

    public static void stop() throws RemoteException, NotBoundException {
        lifecycle.fireStopping();

        stopped = true;

        logger.info("Server is stopping...");

        registry.unbind("BusinessLogicsLoader");
        registry.unbind("AppTerminal");

        registry = null;
        BL = null;

        //убиваем поток RMI, а то зависает
        killRmiThread();

        synchronized (serviceMonitor) {
            serviceMonitor.notify();
        }
    }

    private static void killRmiThread() {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if ("RMI Reaper".equals(t.getName())) {
                t.interrupt();
            }
        }
    }

    // -------------------------------
    // интерфейс для старта через jsvc
    // -------------------------------

    public static void init(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, JRException {
    }

    public static void destroy() {
    }

    // ----------------------------------
    // интерфейс для старта через procrun
    // ----------------------------------

    public static void start(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, JRException {
        start();
    }

    public static void stop(String[] args) throws RemoteException, NotBoundException {
        stop();
    }

    // -----------------------------
    // интерфейс для обычного старта
    // -----------------------------

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, JRException {
        start();
    }
}
