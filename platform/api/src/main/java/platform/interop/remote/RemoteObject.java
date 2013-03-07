package platform.interop.remote;

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

    public RemoteObject(int port, boolean autoExport) throws RemoteException {
        exportPort = port;

        if (autoExport) {
            UnicastRemoteObject.exportObject(this, port);
        }
    }

    public int getExportPort() {
        return exportPort;
    }
}
