package lsfusion.base.remote;

import org.apache.log4j.Logger;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;

public class RMIUtils {
    public static <T extends Remote> T rmiLookup(String host, int port, String name, String subName) throws RemoteException, NotBoundException {
//        return (T) getRegistry(host, port).lookup(name + "/" + subName);
        ZipClientSocketFactory.threadRealHostName.set(host);
        try {
            Registry registry = LocateRegistry.getRegistry(host, port, new ZipClientSocketFactory(host));
            return (T) registry.lookup(name + "/" + subName);
        } finally {
            ZipClientSocketFactory.threadRealHostName.set(null);
        }
    }

    public static void rmiExport(Remote object, int port) throws RemoteException {
//        UnicastRemoteObject.exportObject(object, port);
        UnicastRemoteObject.exportObject(object, port, ZipClientSocketFactory.export, ZipServerSocketFactory.instance);
    }

    // rmi registry should have the same socketfactory as export, because we will export objects on the same port (and with different socket factory we will get port is already bound)
    public static Registry createRmiRegistry(int exportPort) throws RemoteException {
//        return LocateRegistry.createRegistry(exportPort);
        return LocateRegistry.createRegistry(exportPort, ZipClientSocketFactory.export, ZipServerSocketFactory.instance);
    }

    // it's not possible to use external rmi registry because on client we don't know what socketfactory was used (of course we can try default and then ZipClientSocketFactory.export, but it's not that safe)
//    public static Registry getRmiRegistry(int exportPort) throws RemoteException {
//        return LocateRegistry.getRegistry(null, exportPort);
//    }

    public static void killRmiThread() {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if ("RMI Reaper".equals(t.getName())) {
                t.interrupt();
            }
        }
    }
}
