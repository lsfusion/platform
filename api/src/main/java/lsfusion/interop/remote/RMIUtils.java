package lsfusion.interop.remote;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;

import static lsfusion.base.ApiResourceBundle.getString;

public class RMIUtils {
    private static final Logger logger = Logger.getLogger(RMIUtils.class);

    public static void installRmiErrorHandler() throws IOException {
        RMISocketFactory.setFailureHandler(new RMIFailureHandler() {
            public boolean failure(Exception ex) {
                logger.error(getString("exceptions.rmi.error") + " ", ex);
                return true;
            }
        });
    }

    public static void overrideRMIHostName(String hostName) {
        ZipSocketFactory.getInstance().setOverrideHostName(hostName);
    }

    public static <T extends Remote> T rmiLookup(String registryHost, int registryPort, String name, String subName) throws RemoteException, NotBoundException, MalformedURLException {
        return rmiLookup(registryHost, registryPort, name + "/" + subName);
    }

    public static <T extends Remote> T rmiLookup(String registryHost, int registryPort, String name, String subName, RMIClientSocketFactory csf) throws RemoteException, NotBoundException, MalformedURLException {
        return rmiLookup(registryHost, registryPort, name + "/" + subName, csf);
    }

    public static <T extends Remote> T rmiLookup(String registryHost, int registryPort, String name) throws RemoteException, NotBoundException, MalformedURLException {
        return rmiLookup(registryHost, registryPort, name, ZipSocketFactory.getInstance());
    }

    public static <T extends Remote> T rmiLookup(String registryHost, int registryPort, String name, RMIClientSocketFactory csf) throws RemoteException, NotBoundException, MalformedURLException {
        Registry registry = LocateRegistry.getRegistry(registryHost, registryPort, csf);
        return (T) registry.lookup(name);
    }

    public static void rmiExport(Remote object, int port) throws RemoteException {
        UnicastRemoteObject.exportObject(object, port, ZipSocketFactory.getInstance(), ZipSocketFactory.getInstance());
    }

    public static Registry createRmiRegistry(int exportPort) throws RemoteException {
        return LocateRegistry.createRegistry(exportPort, ZipSocketFactory.getInstance(), ZipSocketFactory.getInstance());
    }

    public static Registry getRmiRegistry(int exportPort) throws RemoteException {
        return LocateRegistry.getRegistry(null, exportPort, ZipSocketFactory.getInstance());
    }

    public static void killRmiThread() {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if ("RMI Reaper".equals(t.getName())) {
                t.interrupt();
            }
        }
    }
}
