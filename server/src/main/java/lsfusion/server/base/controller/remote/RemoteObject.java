package lsfusion.server.base.controller.remote;

import lsfusion.base.remote.RMIUtils;
import lsfusion.base.remote.ZipClientSocketFactory;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteObject implements Remote {

    protected final int exportPort;

    public RemoteObject() {
        exportPort = -1;
    }

    public RemoteObject(int port) throws RemoteException {
        //не экспортим по умолчанию
        this(port, false);
    }

    public static void export(Remote object, int port) throws RemoteException {
        RMIUtils.rmiExport(object, port, ZipServerSocketFactory.getInstance());
    }

    public RemoteObject(int port, boolean autoExport) throws RemoteException {
        exportPort = port;

        if (autoExport) {
            export(this, port);
        }
    }

    public int getExportPort() {
        return exportPort;
    }
    
    public final void unexport() {
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException ignore) {
        }
    }
}
