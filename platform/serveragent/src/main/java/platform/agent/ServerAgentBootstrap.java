package platform.agent;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import platform.base.SystemUtils;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import static platform.base.SystemUtils.initRMICompressedSocketFactory;

public class ServerAgentBootstrap {

    private static final String SETTINGS_PATH = "conf/settings.xml";

    private static final Object serviceMonitor = new Object();

    private static FileSystemXmlApplicationContext springContext;

    private static Registry registry;

    private static boolean stopped = false;

    public ServerAgentBootstrap() {
    }

    protected final static Logger logger = Logger.getLogger(ServerAgentBootstrap.class);

    private static ServerAgent serverAgent;

    public static void start() throws IOException {
        logger.info("Server Agent is starting...");

        initRMICompressedSocketFactory();

        initSprintContext();

        initServerAgent();

        initRMIRegistry();

        initServerAgentLocator();

        logger.info("Server Agent has successfully started");

        waitForStopSingal();

        logger.info("Server Agent has stopped");
    }

    private static void initSprintContext() {
        springContext = new FileSystemXmlApplicationContext(SETTINGS_PATH);
    }

    private static void initServerAgent() {
        try {
            serverAgent = (ServerAgent) springContext.getBean("serverAgent");
        } catch (BeanCreationException bce) {
            Throwables.propagate(bce);
        }
    }

    private static void initRMIRegistry() throws RemoteException {
        registry = LocateRegistry.createRegistry(serverAgent.getExportPort());
        try {
            registry.bind("ServerAgent", serverAgent);
        } catch (AlreadyBoundException e) {
            throw new RuntimeException("The service is already started");
        }
    }

    private static void initServerAgentLocator() {
        if (springContext.containsBean("serverAgentLocator")) {
            ServerAgentLocator serverLocator = (ServerAgentLocator) springContext.getBean("serverAgentLocator");
            serverLocator.start();

            logger.info("Server Agent locator has successfully started");
        }
    }

    private static void waitForStopSingal() {
        synchronized (serviceMonitor) {
            while (!stopped) {
                try {
                    serviceMonitor.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private static void notifyStopSingal() {
        stopped = true;
        synchronized (serviceMonitor) {
            serviceMonitor.notify();
        }
    }

    public static void stop() throws RemoteException, NotBoundException {
        logger.info("Server Agent is stopping...");

        registry.unbind("ServerAgent");

        registry = null;
        serverAgent = null;

        //убиваем поток RMI, а то зависает
        SystemUtils.killRmiThread();

        notifyStopSingal();
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

    public static void stop(String[] args) throws RemoteException, NotBoundException {
        stop();
    }

    // -----------------------------
    // интерфейс для обычного старта
    // -----------------------------
    public static void main(String[] args) throws IOException {
        start();
    }
}
