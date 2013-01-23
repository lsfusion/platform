package platform.server.logics;

import net.sf.jasperreports.engine.JRException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import platform.base.ClassUtils;
import platform.base.ExceptionUtils;
import platform.base.col.lru.LRUCache;
import platform.interop.RemoteServerAgentLoaderInterface;
import platform.server.Settings;
import platform.server.lifecycle.LifecycleManager;

import java.io.IOException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.text.MessageFormat;

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
    public static Registry registry;

    public static void start() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, JRException {
        System.setProperty("mail.mime.encodefilename", "true");

        logger.info("Server is starting...");

        stopped = false;

        initRMI();

        initSpringContext();

//        initLRUCaches();

        initLifecycleManager();

        lifecycle.fireStarting();

        createRMIRegistry();

        boolean blCreated = true;
        try {
            BL = (BusinessLogics) springContext.getBean("businessLogics");
        } catch (BeanCreationException bce) {
            Throwable nonSpringCause = ExceptionUtils.getNonSpringCause(bce);
            if (nonSpringCause instanceof BusinessLogics.ScriptErrorsException) {
                logger.error("Error, while creating bl: " + nonSpringCause.getMessage());
            } else {
                logger.error("Exception while creating business logic: ", bce);
            }
            blCreated = false;

            lifecycle.fireError("Error, while creating bl: " + nonSpringCause.getMessage());
        }

        if (blCreated) {
            lifecycle.fireBlCreated(BL);

            initRMIRegistry();

            initServerAgent();

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

    private static void initRMI() throws IOException {
        // делаем, чтобы сборщик мусора срабатывал каждую минуту - для удаления ненужных connection'ов

        if (System.getProperty("sun.rmi.dgc.server.gcInterval") == null) {
            System.setProperty("sun.rmi.dgc.server.gcInterval", "600000");
        }

//        String isDebug = System.getProperty(PLATFORM_SERVER_ISDEBUG);
//        if (isDebug == null || !isDebug.equals("true")) {
//            System.setProperty("java.rmi.dgc.leaseValue", "120000");
//        }

        ClassUtils.initRMICompressedSocketFactory();
    }

    private static void createRMIRegistry() throws RemoteException {
        if (springContext.containsBean("exportPort")) {
            Integer exportPort = (Integer) springContext.getBean("exportPort");
            registry = getRegistry(exportPort);
        }
    }

    private static void initRMIRegistry() throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, JRException {
        if (registry == null)
            registry = getRegistry(BL.getExportPort());

        try {
            String dbName = BL.getDbName() == null ? "default" : BL.getDbName();
            registry.bind(dbName + "/AppTerminal", new ApplicationTerminalImpl(BL.getExportPort()));
            registry.bind(dbName + "/BusinessLogicsLoader", new BusinessLogicsLoader(BL));
        } catch (AlreadyBoundException e2) {
            throw new RuntimeException("The base is already started");
        }
    }

    private static Registry getRegistry(int exportPort) throws RemoteException {
        registry = LocateRegistry.getRegistry(exportPort);
        try {
            registry.list();
        } catch (RemoteException e) {
            registry = LocateRegistry.createRegistry(exportPort);
        }
        return registry;
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

    private static void initServerAgent() {

        try {
            String hostPort = "";
            if (springContext.containsBean("serverAgentServer")) {
                hostPort = (String) springContext.getBean("serverAgentServer");
            }
            RemoteServerAgentLoaderInterface remoteLoader = (RemoteServerAgentLoaderInterface) Naming.lookup(MessageFormat.format("rmi://{0}/ServerAgentLoader", hostPort));
            remoteLoader.setDbName(BL.getDbName() == null ? "default" : BL.getDbName());
        } catch (ConnectException e) {
            //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            logger.error("Unhandled exception : ", e);
        }
    }

    private static void initLRUCaches() {
        LRUCache.init(new int[] { Settings.instance.getLRUOftenCleanPeriod(), Settings.instance.getLRURareCleanPeriod()},
                      new int[] { Settings.instance.getLRUOftenExpireSecond(), Settings.instance.getLRURareExpireSecond()},
                      new int[] { Settings.instance.getLRUOftenProceedBucket(), Settings.instance.getLRURareProceedBucket()});
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

    public static boolean isDebug() {
        return "true".equals(System.getProperty(PLATFORM_SERVER_ISDEBUG));
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
