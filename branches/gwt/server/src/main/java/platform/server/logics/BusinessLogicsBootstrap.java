package platform.server.logics;

import net.sf.jasperreports.engine.JRException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;
import platform.interop.remote.ServerSocketFactory;
import platform.server.Settings;
import platform.server.net.ServerInstanceLocator;
import platform.server.net.ServerInstanceLocatorSettings;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;
import java.sql.SQLException;

public class BusinessLogicsBootstrap {

    public BusinessLogicsBootstrap() {
    }

    protected final static Logger logger = Logger.getLogger(BusinessLogicsBootstrap.class);

    private static Boolean stopped = false;
    private static final Object serviceMonitor = new Object();

    private static BusinessLogics BL;
    private static Registry registry;

    private static void initRMISocketFactory() throws IOException {
        RMISocketFactory socketFactory = RMISocketFactory.getSocketFactory();
        if (socketFactory == null) {
            socketFactory = RMISocketFactory.getDefaultSocketFactory();
        }

        socketFactory = new ServerSocketFactory();

        RMISocketFactory.setFailureHandler(new RMIFailureHandler() {

            public boolean failure(Exception ex) {
                return true;
            }
        });

        RMISocketFactory.setSocketFactory(socketFactory);
    }

    // -------------------------------
    // интерфейс для старта через jsvc
    // -------------------------------

    public static void init(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, JRException {
    }

    public static void start() throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, JRException {

        // делаем, чтобы сборщик мусора срабатывал каждую минуту - для удаления ненужных connection'ов

        if (System.getProperty("sun.rmi.dgc.server.gcInterval") == null) {
            System.setProperty("sun.rmi.dgc.server.gcInterval", "600000");
        }

        System.setProperty("mail.mime.encodefilename", "true");

        initRMISocketFactory();

        stopped = false;

        logger.info("Server is starting...");

        XmlBeanFactory factory = new XmlBeanFactory(new FileSystemResource("conf/settings.xml"));

        if (factory.containsBean("settings")) {
            Settings.instance = (Settings) factory.getBean("settings");
        } else {
            Settings.instance = new Settings();
        }

        BL = (BusinessLogics) factory.getBean("businessLogics");
        registry = LocateRegistry.createRegistry(BL.getExportPort());

//        registry.rebind("BusinessLogics", BL);
        registry.rebind("BusinessLogicsLoader", new BusinessLogicsLoader(BL));

        logger.info("Server has successfully started");

        if (factory.containsBean("serverInstanceLocatorSettings")) {
            ServerInstanceLocatorSettings settings = (ServerInstanceLocatorSettings) factory.getBean("serverInstanceLocatorSettings");
            new ServerInstanceLocator().start(settings, BL.getExportPort());

            logger.info("Server instance locator successfully started");
        }

        synchronized (serviceMonitor) {
            while (!stopped) {
                try {
                    serviceMonitor.wait();
                } catch (InterruptedException e) {
                }
            }
        }

        logger.info("Server has successfully stopped");
    }

    public static void stop() throws RemoteException, NotBoundException {

        stopped = true;

        logger.info("Server is stopping...");

        registry.unbind("BusinessLogicsLoader");

        registry = null;
        BL = null;

        synchronized (serviceMonitor) {
            serviceMonitor.notify();
        }
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
